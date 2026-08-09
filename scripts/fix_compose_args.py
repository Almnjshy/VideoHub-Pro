#!/usr/bin/env python3
"""
Fix Compose positional argument errors in VideoHub Pro Kotlin files.

Converts positional args to named args for:
- Text("text", Color, sp, FontWeight, ...) → Text("text", color = ..., fontSize = ..., fontWeight = ..., ...)
- Icon(imageVector, null, Color, Modifier) → Icon(imageVector, contentDescription = null, tint = Color, modifier = Modifier)
- Surface(Color, Shape, Modifier) → Surface(color = Color, shape = Shape, modifier = Modifier)
- CircularProgressIndicator(Color, Modifier) → CircularProgressIndicator(color = Color, modifier = Modifier)
"""
import re
import sys

FILES = [
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/search/SearchScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/discover/DiscoverScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/library/LibraryScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/notifications/NotificationsScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/diagnostics/DiagnosticsScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/plugins/PluginsScreen.kt",
]

def fix_icon_positional(content):
    """Fix Icon(imageVector, null/String, Color, Modifier...) → named args.
    Pattern: Icon(Icons.Default.X, null, Color(...), Modifier....)
    The 2nd arg is contentDescription (String?), 3rd is tint (Color), 4th is modifier.
    """
    # Pattern: Icon(<imagevector>, <contentDesc>, <color>, <modifier>...)
    # We need to be careful — only fix when args 3 and 4 are Color and Modifier respectively
    # Use a regex that captures: Icon( <vec>, <desc>, <color>, <mod> )
    # where <color> starts with Color( or is a color val, and <mod> starts with Modifier.

    # Simple pattern: Icon(X, Y, Color(...), Modifier....)
    # Match: Icon( <vec>, <null|str>, <color>, <modifier> )
    pattern = r'Icon\(([^,]+),\s*(null|"[^"]*"),\s*(Color\([^)]*\)(?:\.copy\([^)]*\))?|[\w]+(?:\.copy\([^)]*\))?),\s*(Modifier\.[^),]+(?:\.[^),]+)*)\)'
    def replace(m):
        vec = m.group(1).strip()
        desc = m.group(2).strip()
        color = m.group(3).strip()
        mod = m.group(4).strip()
        return f'Icon({vec}, contentDescription = {desc}, tint = {color}, modifier = {mod})'
    content = re.sub(pattern, replace, content)

    # Also handle: Icon(X, Y, Color, Modifier) where Modifier has no dots after (just Modifier.size)
    # Already covered above.

    return content


def fix_surface_positional(content):
    """Fix Surface(Color, Shape, Modifier...) → named args.
    Pattern: Surface(<color>, <shape>, <modifier>...)
    """
    # Surface(Color(...), RoundedCornerShape(...), Modifier....)
    # Need to handle nested parens in RoundedCornerShape and Modifier chain
    # Use a more careful approach — find Surface( and parse balanced parens

    result = []
    i = 0
    while i < len(content):
        idx = content.find('Surface(', i)
        if idx == -1:
            result.append(content[i:])
            break
        # Append everything before Surface(
        result.append(content[i:idx])

        # Find the matching closing paren
        start = idx + len('Surface(')
        depth = 1
        j = start
        args = []
        current_arg = []
        while j < len(content) and depth > 0:
            c = content[j]
            if c == '(':
                depth += 1
                current_arg.append(c)
            elif c == ')':
                depth -= 1
                if depth == 0:
                    if current_arg:
                        args.append(''.join(current_arg))
                    break
                current_arg.append(c)
            elif c == ',' and depth == 1:
                args.append(''.join(current_arg))
                current_arg = []
            else:
                current_arg.append(c)
            j += 1

        # j is now at the closing )
        # Check if this looks like positional Surface(color, shape, modifier)
        # If any arg already has '=', skip (it's using named args)
        has_named = any('=' in a and not a.strip().startswith('//') for a in args)
        # Also skip if it's Surface(modifier = ...) form (first arg is modifier =)
        if has_named or len(args) < 2:
            # Keep as-is
            result.append(content[idx:j+1])
        else:
            # Try to identify: color, shape, modifier
            # color: starts with Color( or is a color val (AmberPrimary, DarkBgCard, etc.)
            # shape: starts with RoundedCornerShape( or is a Shape
            # modifier: starts with Modifier.
            # Also handle: Surface(color, shape) — 2 args
            # Also: Surface(color) — 1 arg (just color)
            named_args = []
            used = [False] * len(args)
            for k, arg in enumerate(args):
                arg_stripped = arg.strip()
                if not used[k]:
                    if arg_stripped.startswith('Color(') or arg_stripped.startswith('AmberPrimary') or \
                       arg_stripped.startswith('DarkBg') or arg_stripped.startswith('Emerald') or \
                       arg_stripped.startswith('RedError') or arg_stripped.startswith('Color.') or \
                       arg_stripped.startswith('Blue') or arg_stripped.startswith('if (') or \
                       arg_stripped.startswith('healthColor') or arg_stripped.startswith('statusColor') or \
                       arg_stripped.startswith('color'):
                        named_args.append(f'color = {arg_stripped}')
                        used[k] = True
                    elif arg_stripped.startswith('RoundedCornerShape'):
                        named_args.append(f'shape = {arg_stripped}')
                        used[k] = True
                    elif arg_stripped.startswith('Modifier'):
                        named_args.append(f'modifier = {arg_stripped}')
                        used[k] = True

            # For any unused args, add them as-is (might be contentColor = or other)
            for k, arg in enumerate(args):
                if not used[k]:
                    named_args.append(arg.strip())

            result.append('Surface(' + ', '.join(named_args) + ')')

        i = j + 1

    return ''.join(result)


def fix_circular_progress_positional(content):
    """Fix CircularProgressIndicator(Color, Modifier) → named args."""
    # CircularProgressIndicator(Color(0xFFF59E0B), Modifier.size(36.dp))
    pattern = r'CircularProgressIndicator\((Color\([^)]*\)(?:\.copy\([^)]*\))?|[\w]+),\s*(Modifier\.[^),]+(?:\.[^),]+)*)\)'
    def replace(m):
        color = m.group(1).strip()
        mod = m.group(2).strip()
        return f'CircularProgressIndicator(color = {color}, modifier = {mod})'
    return re.sub(pattern, replace, content)


def fix_text_positional(content):
    """Fix Text("text", Color, sp, FontWeight, ...) → named args.
    This is the trickiest because Text has many positional args.
    Pattern: Text(<string>, <color>, <sp>, <fontWeight>, <modifier>, ...)
    """
    result = []
    i = 0
    while i < len(content):
        idx = content.find('Text(', i)
        if idx == -1:
            result.append(content[i:])
            break
        # Make sure it's Text( not TextField( or TextButton(
        # Check the char before
        before = content[max(0, idx-1):idx]
        if before.isalnum() or before == '_':
            # Part of a longer identifier — skip
            result.append(content[i:idx+4])
            i = idx + 4
            continue

        result.append(content[i:idx])

        # Find matching closing paren
        start = idx + len('Text(')
        depth = 1
        j = start
        args = []
        current_arg = []
        in_string = False
        string_char = None
        while j < len(content) and depth > 0:
            c = content[j]
            if in_string:
                current_arg.append(c)
                if c == string_char and content[j-1] != '\\':
                    in_string = False
            elif c == '"' or c == "'":
                in_string = True
                string_char = c
                current_arg.append(c)
            elif c == '(':
                depth += 1
                current_arg.append(c)
            elif c == ')':
                depth -= 1
                if depth == 0:
                    if current_arg:
                        args.append(''.join(current_arg))
                    break
                current_arg.append(c)
            elif c == ',' and depth == 1:
                args.append(''.join(current_arg))
                current_arg = []
            else:
                current_arg.append(c)
            j += 1

        # Check if already has named args (color =, fontSize =, etc.)
        has_named = any(re.match(r'\s*\w+\s*=', a) for a in args)
        if has_named or len(args) <= 1:
            # Keep as-is — either already named or just a single string arg
            result.append(content[idx:j+1])
        else:
            # Parse positional args:
            # 1st: text (String)
            # 2nd: color (Color) — starts with Color( or is a color val
            # 3rd: fontSize (TextUnit) — ends with .sp or .em
            # 4th: fontWeight (FontWeight) — starts with FontWeight.
            # 5th: could be modifier (Modifier.) or maxLines or overflow etc.
            named_args = [args[0].strip()]  # text is first
            used = [False] * len(args)
            used[0] = True

            for k in range(1, len(args)):
                arg = args[k].strip()
                if used[k]:
                    continue
                # color
                if not used[k] and (arg.startswith('Color(') or arg in ['AmberPrimary', 'DarkTextPrimary',
                    'DarkTextSecondary', 'DarkBorder', 'EmeraldSuccess', 'RedError', 'Color.White',
                    'Color.Black', 'Color.Transparent'] or arg.startswith('DarkBg') or
                    arg.startswith('Amber') or arg.startswith('Emerald') or arg.startswith('Red') or
                    arg.startswith('healthColor') or arg.startswith('statusColor') or arg.startswith('color')):
                    named_args.append(f'color = {arg}')
                    used[k] = True
                # fontSize (.sp)
                elif not used[k] and re.search(r'\.sp$', arg):
                    named_args.append(f'fontSize = {arg}')
                    used[k] = True
                # fontWeight
                elif not used[k] and arg.startswith('FontWeight.'):
                    named_args.append(f'fontWeight = {arg}')
                    used[k] = True
                # modifier
                elif not used[k] and arg.startswith('Modifier.'):
                    named_args.append(f'modifier = {arg}')
                    used[k] = True

            # Add any remaining unused args as-is
            for k in range(1, len(args)):
                if not used[k]:
                    named_args.append(args[k].strip())

            result.append('Text(' + ', '.join(named_args) + ')')

        i = j + 1

    return ''.join(result)


for filepath in FILES:
    print(f"Processing {filepath}...")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    original = content
    content = fix_icon_positional(content)
    content = fix_surface_positional(content)
    content = fix_circular_progress_positional(content)
    content = fix_text_positional(content)
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ Fixed")
    else:
        print(f"  (no changes)")

print("Done!")

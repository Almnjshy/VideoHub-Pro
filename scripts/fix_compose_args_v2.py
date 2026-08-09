#!/usr/bin/env python3
"""
Fix remaining positional Surface() calls.
Surface(Color, Shape, Modifier) → Surface(color = Color, shape = Shape, modifier = Modifier)
Also fixes remaining positional Icon() and Text() calls.
"""
import re

FILES = [
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/search/SearchScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/discover/DiscoverScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/library/LibraryScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/notifications/NotificationsScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/diagnostics/DiagnosticsScreen.kt",
    "/home/z/my-project/android/app/src/main/java/com/videohub/pro/ui/screens/plugins/PluginsScreen.kt",
]

def find_balanced_parens(content, start):
    """Find the matching closing paren for the opening paren at start."""
    depth = 1
    j = start
    in_string = False
    string_char = None
    while j < len(content) and depth > 0:
        c = content[j]
        if in_string:
            if c == string_char and content[j-1] != '\\':
                in_string = False
        elif c == '"' or c == "'":
            in_string = True
            string_char = c
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return j
        j += 1
    return -1

def split_args(args_str):
    """Split a comma-separated arg string, respecting nested parens and strings."""
    args = []
    current = []
    depth = 0
    in_string = False
    string_char = None
    for c in args_str:
        if in_string:
            current.append(c)
            if c == string_char:
                in_string = False
        elif c == '"' or c == "'":
            in_string = True
            string_char = c
            current.append(c)
        elif c == '(':
            depth += 1
            current.append(c)
        elif c == ')':
            depth -= 1
            current.append(c)
        elif c == ',' and depth == 0:
            args.append(''.join(current))
            current = []
        else:
            current.append(c)
    if current:
        args.append(''.join(current))
    return [a.strip() for a in args]

def is_color_arg(arg):
    """Check if an arg looks like a Color."""
    return (arg.startswith('Color(') or arg.startswith('Color.') or
            arg.startswith('AmberPrimary') or arg.startswith('DarkBg') or
            arg.startswith('EmeraldSuccess') or arg.startswith('RedError') or
            arg.startswith('healthColor') or arg.startswith('statusColor') or
            arg.startswith('color') or arg.startswith('if (') and 'Color' in arg or
            arg.startswith('if (mf.isVideo)') or arg.startswith('if (isFav)'))

def is_shape_arg(arg):
    """Check if an arg looks like a Shape."""
    return arg.startswith('RoundedCornerShape')

def is_modifier_arg(arg):
    """Check if an arg looks like a Modifier."""
    return arg.startswith('Modifier.')

def is_named_arg(arg):
    """Check if an arg is already named (e.g. color = ...)."""
    return bool(re.match(r'^\w+\s*=', arg))

def fix_surface_calls(content):
    """Fix all Surface(...) calls with positional (color, shape, modifier) args."""
    result = []
    i = 0
    while i < len(content):
        idx = content.find('Surface(', i)
        if idx == -1:
            result.append(content[i:])
            break
        # Make sure it's not part of a longer identifier
        before = content[max(0, idx-1):idx]
        if before.isalnum() or before == '_':
            result.append(content[i:idx+8])
            i = idx + 8
            continue
        result.append(content[i:idx])
        start = idx + len('Surface(')
        end = find_balanced_parens(content, start)
        if end == -1:
            result.append(content[idx:])
            break
        args_str = content[start:end]
        args = split_args(args_str)
        # Check if first 3 args are positional color, shape, modifier
        if (len(args) >= 3 and
            is_color_arg(args[0]) and
            is_shape_arg(args[1]) and
            is_modifier_arg(args[2]) and
            not is_named_arg(args[0])):
            # Convert to named
            new_args = [f'color = {args[0]}', f'shape = {args[1]}', f'modifier = {args[2]}']
            new_args.extend(args[3:])
            result.append('Surface(' + ', '.join(new_args) + ')')
        elif (len(args) >= 2 and
              is_color_arg(args[0]) and
              is_shape_arg(args[1]) and
              not is_named_arg(args[0])):
            # Surface(color, shape) — 2 args
            new_args = [f'color = {args[0]}', f'shape = {args[1]}']
            new_args.extend(args[2:])
            result.append('Surface(' + ', '.join(new_args) + ')')
        else:
            result.append(content[idx:end+1])
        i = end + 1
    return ''.join(result)

def fix_icon_calls(content):
    """Fix remaining Icon(vec, desc, color, modifier) calls."""
    result = []
    i = 0
    while i < len(content):
        idx = content.find('Icon(', i)
        if idx == -1:
            result.append(content[i:])
            break
        before = content[max(0, idx-1):idx]
        if before.isalnum() or before == '_':
            result.append(content[i:idx+4])
            i = idx + 4
            continue
        result.append(content[i:idx])
        start = idx + len('Icon(')
        end = find_balanced_parens(content, start)
        if end == -1:
            result.append(content[idx:])
            break
        args_str = content[start:end]
        args = split_args(args_str)
        # Icon(imageVector, contentDescription, tint, modifier)
        if (len(args) >= 4 and
            not is_named_arg(args[0]) and
            not is_named_arg(args[1]) and
            is_color_arg(args[2]) and
            is_modifier_arg(args[3]) and
            not is_named_arg(args[2])):
            new_args = [args[0], f'contentDescription = {args[1]}', f'tint = {args[2]}', f'modifier = {args[3]}']
            new_args.extend(args[4:])
            result.append('Icon(' + ', '.join(new_args) + ')')
        else:
            result.append(content[idx:end+1])
        i = end + 1
    return ''.join(result)

def fix_text_calls(content):
    """Fix remaining Text("str", Color, sp, ...) calls."""
    result = []
    i = 0
    while i < len(content):
        idx = content.find('Text(', i)
        if idx == -1:
            result.append(content[i:])
            break
        before = content[max(0, idx-1):idx]
        if before.isalnum() or before == '_':
            result.append(content[i:idx+4])
            i = idx + 4
            continue
        result.append(content[i:idx])
        start = idx + len('Text(')
        end = find_balanced_parens(content, start)
        if end == -1:
            result.append(content[idx:])
            break
        args_str = content[start:end]
        args = split_args(args_str)
        # Text(text, color, fontSize, fontWeight, ...)
        if (len(args) >= 2 and
            not is_named_arg(args[0]) and
            is_color_arg(args[1]) and
            not is_named_arg(args[1])):
            new_args = [args[0], f'color = {args[1]}']
            used = [False] * len(args)
            used[0] = True
            used[1] = True
            for k in range(2, len(args)):
                arg = args[k]
                if is_named_arg(arg):
                    new_args.append(arg)
                    used[k] = True
                elif re.search(r'\.sp$', arg) and not used[k]:
                    new_args.append(f'fontSize = {arg}')
                    used[k] = True
                elif arg.startswith('FontWeight.') and not used[k]:
                    new_args.append(f'fontWeight = {arg}')
                    used[k] = True
                elif is_modifier_arg(arg) and not used[k]:
                    new_args.append(f'modifier = {arg}')
                    used[k] = True
                elif not used[k]:
                    new_args.append(arg)
                    used[k] = True
            result.append('Text(' + ', '.join(new_args) + ')')
        else:
            result.append(content[idx:end+1])
        i = end + 1
    return ''.join(result)

def fix_circular_progress(content):
    """Fix CircularProgressIndicator(color, modifier) calls."""
    result = []
    i = 0
    while i < len(content):
        idx = content.find('CircularProgressIndicator(', i)
        if idx == -1:
            result.append(content[i:])
            break
        result.append(content[i:idx])
        start = idx + len('CircularProgressIndicator(')
        end = find_balanced_parens(content, start)
        if end == -1:
            result.append(content[idx:])
            break
        args_str = content[start:end]
        args = split_args(args_str)
        if (len(args) >= 2 and
            is_color_arg(args[0]) and
            is_modifier_arg(args[1]) and
            not is_named_arg(args[0])):
            new_args = [f'color = {args[0]}', f'modifier = {args[1]}']
            new_args.extend(args[2:])
            result.append('CircularProgressIndicator(' + ', '.join(new_args) + ')')
        else:
            result.append(content[idx:end+1])
        i = end + 1
    return ''.join(result)

for filepath in FILES:
    print(f"Processing {filepath}...")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    original = content
    content = fix_surface_calls(content)
    content = fix_icon_calls(content)
    content = fix_text_calls(content)
    content = fix_circular_progress(content)
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ Fixed")
    else:
        print(f"  (no changes)")

print("Done!")

"""
VideoHub Pro — Embedded yt-dlp resolver (Chaquopy 17 / Python 3.11)
يعمل داخل تطبيق أندرويد عبر Chaquopy — لا حاجة لخادم خارجي.

الاستخدام من Kotlin:
    from com.chaquo.python import Python
    py = Python.getInstance()
    result_json = py.getModule("resolver").callAttr("resolve", url, cookies_json).toString()

الـ resolver يُرجع JSON يحتوي على:
  - {"ok": true, "metadata": {...}, "formats": [...]} عند النجاح
  - {"ok": false, "error": "...", "errorType": "..."} عند الفشل
"""

import json
import os
import sys
import time
import tempfile
from typing import Any, Dict, List, Optional


# ============ Platform-specific cookie domains ============

COOKIE_DOMAINS = {
    'youtube': ['.youtube.com', '.google.com'],
    'facebook': ['.facebook.com', '.m.facebook.com'],
    'tiktok': ['.tiktok.com'],
    'instagram': ['.instagram.com'],
    'x': ['.twitter.com', '.x.com'],
    'reddit': ['.reddit.com'],
    'twitch': ['.twitch.tv'],
    'vimeo': ['.vimeo.com'],
    'dailymotion': ['.dailymotion.com'],
    'soundcloud': ['.soundcloud.com'],
    'pinterest': ['.pinterest.com'],
    'linkedin': ['.linkedin.com'],
    'tumblr': ['.tumblr.com'],
    'streamable': ['.streamable.com'],
}

# ============ Platform detection ============

def _detect_platform(url: str) -> str:
    """Detect platform from URL."""
    from urllib.parse import urlparse
    host = (urlparse(url).hostname or '').lower()
    if 'youtube' in host or 'youtu.be' in host:
        return 'youtube'
    if 'facebook' in host or 'fb.watch' in host or 'fb.com' in host:
        return 'facebook'
    if 'tiktok' in host:
        return 'tiktok'
    if 'twitter' in host or 'x.com' in host or 't.co' in host:
        return 'x'
    if 'instagram' in host:
        return 'instagram'
    if 'vimeo' in host:
        return 'vimeo'
    if 'dailymotion' in host or 'dai.ly' in host:
        return 'dailymotion'
    if 'reddit' in host:
        return 'reddit'
    if 'twitch' in host:
        return 'twitch'
    if 'soundcloud' in host:
        return 'soundcloud'
    if 'pinterest' in host or 'pin.it' in host:
        return 'pinterest'
    if 'linkedin' in host:
        return 'linkedin'
    if 'tumblr' in host:
        return 'tumblr'
    if 'streamable' in host:
        return 'streamable'
    return 'generic'


# ============ Cookie file generation ============

def _write_cookies_file(cookies: Dict[str, str], platform: str) -> Optional[str]:
    """Write cookies to a Netscape-format file with the correct domain."""
    if not cookies:
        return None

    domains = COOKIE_DOMAINS.get(platform, ['.' + platform + '.com'])
    cookies_file = tempfile.NamedTemporaryFile(
        mode='w', suffix='.txt', delete=False, prefix='vh_cookies_'
    )
    cookies_file.write('# Netscape HTTP Cookie File\n')
    for name, value in cookies.items():
        # Skip internal keys like _domain
        if name.startswith('_'):
            continue
        # Write cookie for each relevant domain
        for domain in domains:
            cookies_file.write(f'{domain}\tTRUE\t/\tFALSE\t0\t{name}\t{value}\n')
    cookies_file.close()
    return cookies_file.name


# ============ yt-dlp options builder ============

def _build_ydl_opts(cookies: Optional[Dict[str, str]], platform: str) -> dict:
    """Build yt-dlp options optimized for each platform."""

    # Base options — strong browser-like headers
    opts = {
        'quiet': True,
        'no_warnings': True,
        'nocheckcertificate': True,
        'ignoreerrors': False,
        'noprogress': True,
        'geo_bypass': True,
        'geo_bypass_country': 'US',
        'socket_timeout': 30,
        'retries': 3,
        # Use a realistic mobile Chrome user agent
        'user_agent': 'Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
        'http_headers': {
            'Accept-Language': 'en-US,en;q=0.9,ar;q=0.8',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Sec-Fetch-Mode': 'navigate',
            'Sec-Fetch-Dest': 'document',
            'Sec-Fetch-Site': 'none',
            'Sec-Ch-Ua': '"Chromium";v="131", "Not_A Brand";v="24"',
            'Sec-Ch-Ua-Mobile': '?1',
            'Sec-Ch-Ua-Platform': '"Android"',
            'Upgrade-Insecure-Requests': '1',
        },
    }

    # Platform-specific extractor args
    extractor_args = {}

    if platform == 'tiktok':
        # TikTok needs specific API hostname and client settings
        extractor_args['tiktok'] = {
            'app_info': 'android',
        }

    elif platform == 'youtube':
        # YouTube: try multiple client types for better compatibility
        extractor_args['youtube'] = {
            'player_client': ['android', 'web', 'ios'],
        }

    elif platform == 'facebook':
        # Facebook: use mobile client for better compatibility
        extractor_args['facebook'] = {
            'video_id': True,
        }

    if extractor_args:
        opts['extractor_args'] = extractor_args

    # Cookies — write to temp file with correct domain
    if cookies:
        cookiefile = _write_cookies_file(cookies, platform)
        if cookiefile:
            opts['cookiefile'] = cookiefile

    return opts


def _cleanup_opts(opts: dict) -> None:
    """Clean up any temp files created in opts."""
    cookiefile = opts.get('cookiefile')
    if cookiefile and os.path.exists(cookiefile):
        try:
            os.unlink(cookiefile)
        except Exception:
            pass


# ============ Quality/format helpers ============

def _map_quality(height: Optional[int], is_audio: bool = False) -> str:
    """Map height to a friendly quality label."""
    if is_audio:
        return 'audio'
    if height is None:
        return 'unknown'
    if height >= 2160:
        return '4k'
    if height >= 1080:
        return '1080p'
    if height >= 720:
        return '720p'
    if height >= 480:
        return '480p'
    if height >= 360:
        return '360p'
    if height >= 240:
        return '240p'
    if height >= 144:
        return '144p'
    return 'unknown'


def _humanize_format(fmt: dict) -> dict:
    """Convert a yt-dlp format dict to the frontend MediaFormat shape."""
    height = fmt.get('height')
    vcodec = fmt.get('vcodec', 'none')
    acodec = fmt.get('acodec', 'none')
    has_video = vcodec and vcodec != 'none'
    has_audio = acodec and acodec != 'none'
    is_audio_only = not has_video and has_audio

    filesize = fmt.get('filesize') or fmt.get('filesize_approx') or 0
    fps = fmt.get('fps')
    ext = fmt.get('ext', 'mp4')
    format_id = fmt.get('format_id', '')
    url = fmt.get('url', '')
    protocol = fmt.get('protocol', '')

    if is_audio_only:
        media_type = 'audio'
        quality = 'audio'
        abr = fmt.get('abr') or 0
        label_parts = ['Audio']
        if abr:
            label_parts.append(f'{int(abr)}kbps')
        if ext:
            label_parts.append(ext.upper())
        label = ' · '.join(label_parts)
    else:
        media_type = 'video'
        quality = _map_quality(height)
        label_parts = []
        if height:
            label_parts.append(f'{height}p')
        if fps and fps > 30:
            label_parts.append(f'{fps}fps')
        if not has_audio:
            label_parts.append('video-only')
        if ext:
            label_parts.append(ext.upper())
        label = ' · '.join(label_parts) if label_parts else f'{format_id}'

    return {
        'id': format_id or f'{ext}-{quality}-{height or 0}',
        'quality': quality,
        'ext': ext,
        'sizeBytes': int(filesize) if filesize else 0,
        'mediaType': media_type,
        'hasAudio': bool(has_audio),
        'bitrate': int(fmt.get('vbr', 0) or 0) + int(fmt.get('abr', 0) or 0) or None,
        'fps': int(fps) if fps else None,
        'label': label,
        'downloadUrl': url,
        'protocol': protocol,
        'height': height,
        'vcodec': vcodec,
        'acodec': acodec,
    }


def _filter_best_formats(formats: list) -> list:
    """Pick a useful subset of formats.

    Strategy: combined video+audio formats are preferred because this build
    of the app does NOT bundle FFmpeg (arthenica/ffmpeg-kit was archived in
    April 2025 and is no longer available from Maven). Video-only + audio-only
    formats are still listed so users can pick them if they have a build with
    FFmpeg support, but combined formats are surfaced first so downloads
    "just work" without a merge step.
    """
    if not formats:
        return []

    audio_only = []
    video_with_audio = []
    video_only = []

    for f in formats:
        vcodec = (f.get('vcodec') or 'none').lower()
        acodec = (f.get('acodec') or 'none').lower()
        has_v = vcodec != 'none'
        has_a = acodec != 'none'

        if not has_v and has_a:
            audio_only.append(f)
        elif has_v and has_a:
            video_with_audio.append(f)
        elif has_v and not has_a:
            video_only.append(f)

    def sort_key(f):
        h = f.get('height') or 0
        abr = f.get('abr') or 0
        return (h, abr)

    video_with_audio.sort(key=sort_key, reverse=True)
    video_only.sort(key=sort_key, reverse=True)
    audio_only.sort(key=lambda f: f.get('abr') or 0, reverse=True)

    result = []
    result.extend(video_with_audio)
    result.extend(video_only[:4])
    result.extend(audio_only[:2])

    # Dedupe by format_id
    seen = set()
    deduped = []
    for f in result:
        fid = f.get('format_id')
        if fid and fid in seen:
            continue
        if fid:
            seen.add(fid)
        deduped.append(f)

    return deduped


# ============ Error classification ============

def _classify_error(err_str: str, platform: str) -> tuple:
    """Classify error and return (error_type, user_message_ar)."""
    err_lower = err_str.lower()

    if 'http error 429' in err_lower or 'too many requests' in err_lower:
        return ('RATE_LIMITED', 'تم حظرك مؤقتاً بسبب كثرة الطلبات. حاول مرة أخرى بعد دقائق.')

    if 'login required' in err_lower or 'private video' in err_lower or 'sign in' in err_lower:
        return ('AUTHENTICATION_REQUIRED',
                f'هذا المحتوى يتطلب تسجيل الدخول إلى {platform}. '
                'افتح الإعدادات وسجّل الدخول إلى المنصة ثم أعد المحاولة.')

    if 'unexpected response' in err_lower and platform == 'tiktok':
        return ('PLATFORM_CHANGED',
                'TikTok حديثاً حدّث نظام الحماية. '
                'حاول تسجيل الدخول إلى TikTok من الإعدادات أولاً، '
                'أو استخدم رابطاً آخر.')

    if 'unexpected response' in err_lower:
        return ('PLATFORM_CHANGED',
                f'منصة {platform} حدّثت نظامها. '
                'حاول تسجيل الدخول من الإعدادات أو استخدم رابطاً آخر.')

    if 'video unavailable' in err_lower or 'removed' in err_lower:
        return ('NOT_FOUND', 'الفيديو غير متاح أو تم حذفه.')

    if 'unsupported url' in err_lower:
        return ('UNSUPPORTED_PLATFORM', 'هذا الرابط غير مدعوم.')

    if 'drm' in err_lower:
        return ('DRM_PROTECTED', 'هذا المحتوى محمي بـ DRM ولا يمكن تنزيله.')

    if 'no video' in err_lower or 'no media' in err_lower:
        return ('NO_MEDIA_STREAM', 'لم يتم العثور على وسائط قابلة للتنزيل في هذا الرابط.')

    return ('EXTRACTION_FAILED', f'فشل في استخراج الوسائط: {err_str[:200]}')


# ============ Main resolve function ============

def resolve(url: str, cookies_json: Optional[str] = None) -> str:
    """
    Resolve a URL using yt-dlp.

    Args:
        url: The media URL to resolve
        cookies_json: Optional JSON string of cookies dict (for authenticated content)

    Returns:
        JSON string with either:
        - {"ok": true, "metadata": {...}, "formats": [...]} on success
        - {"ok": false, "error": "...", "errorType": "..."} on failure
    """
    try:
        import yt_dlp

        platform = _detect_platform(url)

        cookies = None
        if cookies_json:
            try:
                cookies = json.loads(cookies_json)
            except Exception:
                cookies = None

        opts = _build_ydl_opts(cookies, platform)

        try:
            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info(url, download=False)
        finally:
            _cleanup_opts(opts)

        raw_formats = info.get('formats', []) or []
        if not raw_formats and info.get('url'):
            # Single-format extractors (e.g., Streamable)
            raw_formats = [info]

        filtered = _filter_best_formats(raw_formats)
        formats = [_humanize_format(f) for f in filtered]

        # Sort: video formats first by quality desc, then audio
        quality_order = {
            '4k': 7, '1080p': 6, '720p': 5, '480p': 4,
            '360p': 3, '240p': 2, '144p': 1, 'audio': 0, 'unknown': -1
        }

        def sort_format(fmt):
            return (
                0 if fmt['mediaType'] == 'video' else 1,
                -quality_order.get(fmt['quality'], -1),
            )
        formats.sort(key=sort_format)

        metadata = {
            'title': info.get('title') or info.get('fulltitle') or 'Untitled',
            'author': info.get('uploader') or info.get('channel') or info.get('creator'),
            'thumbnailUrl': info.get('thumbnail'),
            'durationSeconds': info.get('duration'),
            'description': (info.get('description') or '')[:1000],
            'sourceUrl': url,
            'platformId': platform,
            'extractor': info.get('extractor_key', '').lower(),
            'formats': formats,
            'resolvedAt': int(time.time() * 1000),
        }

        # If no formats were extracted, try the top-level info as a single format
        if not formats and info.get('url'):
            single = _humanize_format(info)
            single['downloadUrl'] = info['url']
            metadata['formats'] = [single]
            formats = [single]

        return json.dumps({
            'ok': True,
            'metadata': metadata,
            'formatCount': len(formats),
            'extractor': info.get('extractor_key', ''),
        })

    except Exception as e:
        err_str = str(e)
        platform = _detect_platform(url)
        error_type, user_message = _classify_error(err_str, platform)

        return json.dumps({
            'ok': False,
            'error': user_message,
            'errorType': error_type,
            'rawError': err_str[:500],
        })


def get_version() -> str:
    """Return yt-dlp version string."""
    try:
        import yt_dlp
        return yt_dlp.version.__version__
    except Exception:
        return 'unknown'


def health_check() -> str:
    """Check if Python + yt-dlp are working."""
    try:
        import yt_dlp
        return json.dumps({
            'ok': True,
            'python': sys.version.split()[0],
            'yt_dlp': yt_dlp.version.__version__,
            'chaquopy': '17.0.0',
        })
    except Exception as e:
        return json.dumps({
            'ok': False,
            'error': str(e),
        })


def search(query: str, max_results: int = 20) -> str:
    """
    Search YouTube using yt-dlp's ytsearch.
    Returns JSON with search results.
    """
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'user_agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36',
        }

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(f"ytsearch{max_results}:{query}", download=False)

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None:
                continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', entry.get('channel', '')),
                'duration': entry.get('duration'),
                'viewCount': entry.get('view_count'),
                'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
            })

        return json.dumps({
            'ok': True,
            'results': results,
            'count': len(results),
        })

    except Exception as e:
        return json.dumps({
            'ok': False,
            'error': str(e)[:500],
        })


def get_trending() -> str:
    """
    Get trending videos from YouTube.
    Returns JSON with trending results.
    """
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'playlist_items': '1-20',
        }

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info('https://www.youtube.com/feed/trending', download=False)

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None:
                continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', entry.get('channel', '')),
                'duration': entry.get('duration'),
                'view_count': entry.get('view_count'),
                'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
            })

        return json.dumps({
            'ok': True,
            'results': results,
            'count': len(results),
        })

    except Exception as e:
        return json.dumps({
            'ok': False,
            'error': str(e)[:500],
        })


def resolve_playlist(url: str, max_videos: int = 50) -> str:
    """
    Resolve a YouTube playlist and return all video URLs.
    """
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'playlistend': max_videos,
        }

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None:
                continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', ''),
                'duration': entry.get('duration'),
            })

        return json.dumps({
            'ok': True,
            'results': results,
            'count': len(results),
            'playlistTitle': info.get('title', ''),
        })

    except Exception as e:
        return json.dumps({
            'ok': False,
            'error': str(e)[:500],
        })


def search_multi(query: str, platforms: str = "youtube", max_results: int = 10) -> str:
    """
    Search multiple platforms.
    platforms: comma-separated list (e.g., "youtube,tiktok" or "youtube,twitter,instagram")
    """
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'user_agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36',
        }

        all_results = []
        for platform in platforms.split(','):
            platform = platform.strip().lower()
            try:
                if platform == 'youtube':
                    info = yt_dlp.YoutubeDL(opts).extract_info(f"ytsearch{max_results}:{query}", download=False)
                elif platform == 'soundcloud':
                    info = yt_dlp.YoutubeDL(opts).extract_info(f"scsearch{max_results}:{query}", download=False)
                elif platform == 'youtube_music':
                    info = yt_dlp.YoutubeDL(opts).extract_info(f"ytmsearch{max_results}:{query}", download=False)
                else:
                    continue

                entries = info.get('entries', []) or []
                for entry in entries:
                    if entry is None:
                        continue
                    all_results.append({
                        'title': entry.get('title', ''),
                        'url': entry.get('url', entry.get('webpage_url', '')),
                        'uploader': entry.get('uploader', entry.get('channel', '')),
                        'duration': entry.get('duration'),
                        'viewCount': entry.get('view_count'),
                        'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
                        'platform': platform,
                    })
            except Exception:
                continue

        return json.dumps({
            'ok': True,
            'results': all_results,
            'count': len(all_results),
        })

    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:500]})


def get_trending_by_category(category: str = "now") -> str:
    """
    Get trending videos by category.
    Categories: now, music, gaming, movies, news
    """
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'playlist_items': '1-30',
        }

        # Map categories to YouTube trending URLs
        url_map = {
            'now': 'https://www.youtube.com/feed/trending',
            'music': 'https://www.youtube.com/feed/trending?bp=4gINGgt5dG1hX2NoYXJ0cwTCAggDEAEYAyAQM',
            'gaming': 'https://www.youtube.com/feed/trending?bp=4gIcGhpnYW1pbmdfY29ycHVzX21vc3RfcG9wdWxhcgTCAggDEAEYAyAA',
            'movies': 'https://www.youtube.com/feed/trending?bp=4gIKGgh0cmFpbGVycwTCAggDEAEYAyAA',
            'news': 'https://www.youtube.com/feed/trending?bp=4gJKGgh0cmFpbGVyc_QEPBwgDEAEYAyAA',
        }

        url = url_map.get(category, url_map['now'])

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None:
                continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', entry.get('channel', '')),
                'duration': entry.get('duration'),
                'view_count': entry.get('view_count'),
                'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
            })

        return json.dumps({
            'ok': True,
            'results': results,
            'count': len(results),
            'category': category,
        })

    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:500]})


def get_trending_by_category_region(category: str = "now", region: str = "US", max_results: int = 30) -> str:
    """Get trending by category AND region."""
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
            'geo_bypass_country': region,
            'playlist_items': f'1-{max_results}',
        }

        url_map = {
            'now': 'https://www.youtube.com/feed/trending',
            'music': 'https://www.youtube.com/feed/trending?bp=4gINGgt5dG1hX2NoYXJ0cwTCAggDEAEYAyAQM',
            'gaming': 'https://www.youtube.com/feed/trending?bp=4gIcGhpnYW1pbmdfY29ycHVzX21vc3RfcG9wdWxhcgTCAggDEAEYAyAA',
            'movies': 'https://www.youtube.com/feed/trending?bp=4gIKGgh0cmFpbGVycwTCAggDEAEYAyAA',
            'news': 'https://www.youtube.com/feed/trending?bp=4gJKGgh0cmFpbGVyc_QEPBwgDEAEYAyAA',
        }

        url = url_map.get(category, url_map['now'])
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None: continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', entry.get('channel', '')),
                'duration': entry.get('duration'),
                'view_count': entry.get('view_count'),
                'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
                'upload_date': entry.get('upload_date'),
            })

        return json.dumps({'ok': True, 'results': results, 'count': len(results), 'category': category, 'region': region})
    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:500]})


def search_with_filters(query: str, platform: str = "youtube", max_results: int = 15,
                        sort_by: str = "relevance", duration_filter: str = "any",
                        time_filter: str = "any") -> str:
    """Search with filters: sort (relevance/views/date/rating), duration (short/medium/long), time (day/week/month/year)."""
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': True,
            'skip_download': True,
            'geo_bypass': True,
        }

        # Build YouTube search URL with filters
        if platform == "youtube":
            # YouTube search filter format: ytsearch:N:query, or use URL params
            base_url = f"https://www.youtube.com/results?search_query={query}"

            # Sort filter
            sp_sort = {"relevance": "", "views": "CAMSAhAB", "date": "CAI%3D", "rating": "CAE%3D"}
            sp_duration = {"any": "", "short": "EgQQARgB", "medium": "EgQQARgC", "long": "EgQQARgD"}
            sp_time = {"any": "", "day": "EgQIBBAB", "week": "EgQIBRAB", "month": "EgQIBxAB", "year": "EgQICBAB"}

            sp = ""
            if time_filter != "any": sp = sp_time.get(time_filter, "")
            if duration_filter != "any": sp = (sp + sp_duration.get(duration_filter, "")) if sp else sp_duration.get(duration_filter, "")
            if sort_by != "relevance": sp = (sp + sp_sort.get(sort_by, "")) if sp else sp_sort.get(sort_by, "")

            if sp: base_url += f"&sp={sp}"

            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info(base_url, download=False)
        elif platform == "soundcloud":
            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info(f"scsearch{max_results}:{query}", download=False)
        elif platform == "youtube_music":
            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info(f"ytmsearch{max_results}:{query}", download=False)
        else:
            return json.dumps({'ok': False, 'error': f'Unknown platform: {platform}'})

        entries = info.get('entries', []) or []
        results = []
        for entry in entries:
            if entry is None: continue
            results.append({
                'title': entry.get('title', ''),
                'url': entry.get('url', entry.get('webpage_url', '')),
                'uploader': entry.get('uploader', entry.get('channel', '')),
                'duration': entry.get('duration'),
                'view_count': entry.get('view_count'),
                'thumbnail': entry.get('thumbnails', [{}])[0].get('url') if entry.get('thumbnails') else entry.get('thumbnail'),
                'platform': platform,
                'upload_date': entry.get('upload_date'),
            })

        return json.dumps({'ok': True, 'results': results, 'count': len(results)})
    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:500]})


def get_search_suggestions(query: str) -> str:
    """Get search suggestions using YouTube autocomplete API."""
    try:
        import urllib.request
        import urllib.parse
        import json as _json

        url = f"https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q={urllib.parse.quote(query)}&output=json"
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = resp.read().decode('utf-8')

        # YouTube returns JSON with leading code
        if data.startswith('window.google.ac.h('):
            data = data[len('window.google.ac.h('):-1]
        parsed = _json.loads(data)
        suggestions = [item[0] for item in parsed.get('1', [])]

        return json.dumps({'ok': True, 'suggestions': suggestions})
    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:200], 'suggestions': []})


def get_video_info(url: str) -> str:
    """Get detailed video info for preview (without extracting all formats)."""
    try:
        import yt_dlp

        opts = {
            'quiet': True,
            'no_warnings': True,
            'skip_download': True,
            'geo_bypass': True,
            'noplaylist': True,
        }

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)

        return json.dumps({
            'ok': True,
            'title': info.get('title', ''),
            'uploader': info.get('uploader', ''),
            'uploader_id': info.get('uploader_id', ''),
            'upload_date': info.get('upload_date', ''),
            'duration': info.get('duration'),
            'view_count': info.get('view_count'),
            'like_count': info.get('like_count'),
            'description': (info.get('description') or '')[:2000],
            'thumbnail': info.get('thumbnail'),
            'categories': info.get('categories', []),
            'tags': info.get('tags', [])[:10],
            'url': url,
            'platform': _detect_platform(url),
            'formats_count': len(info.get('formats', []) or []),
        })
    except Exception as e:
        return json.dumps({'ok': False, 'error': str(e)[:500]})

import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

// E2E test fixture (?e2e=1 debug mode only): canned innertube player response.
// Points at a real 20s audio file in /public so playback + download are genuine.
export async function GET() {
  return NextResponse.json({
    basic_info: {
      title: 'JixOne Test Track',
      author: 'E2E Artist',
      duration: 20,
    },
    streaming_data: {
      adaptive_formats: [
        {
          itag: 251,
          url: '/test-audio.mp3',
          mimeType: 'audio/mp4',
          bitrate: 128000,
          content_length: 160539,
          has_audio: true,
          has_video: false,
          url_is_encoded: false,
        },
        {
          itag: 140,
          url: '/test-audio.mp3',
          mimeType: 'audio/mp4',
          bitrate: 128000,
          content_length: 160539,
          has_audio: true,
          has_video: false,
        },
      ],
    },
    playability_status: { status: 'OK' },
  });
}

import { NextRequest } from "next/server";

const BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || "http://localhost:8080";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const url = `${BASE_URL}/api/bidding/auctions/${id}/stream`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
    },
    credentials: "include",
  });

  if (!response.ok) {
    return new Response("stream unavailable", { status: 502 });
  }

  const body = response.body;
  if (!body) {
    return new Response("stream unavailable", { status: 502 });
  }

  const stream = new ReadableStream({
    start(controller) {
      const reader = body.getReader();

      function pump() {
        reader.read().then(({ done, value }) => {
          if (done) {
            controller.close();
            return;
          }
          controller.enqueue(value);
          pump();
        }).catch((err) => {
          controller.error(err);
        });
      }

      pump();
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      "Connection": "keep-alive",
      "X-Accel-Buffering": "no",
    },
  });
}

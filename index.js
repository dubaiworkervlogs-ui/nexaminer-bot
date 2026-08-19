export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("NexaMine is online 🚀");
    }

    try {
      const update = await request.json();

      console.log("Telegram update:", JSON.stringify(update));

      if (!update.message) {
        return new Response("OK");
      }

      const chatId = update.message.chat.id;
      const text = update.message.text || "";

      if (text === "/start") {
        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          "⛏️ Welcome to NexaMine!\n\n" +
          "Mine • Earn • Grow 🚀\n\n" +
          "🪙 Balance: 0 NXM\n" +
          "⛏️ Mining: Ready\n\n" +
          "Use /mine to start mining."
        );
      }

      if (text === "/mine") {
        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          "⛏️ Mining started!\n\n🪙 +10 NXM"
        );
      }

      if (text === "/help") {
        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          "NexaMine Commands:\n\n/start — Open NexaMine\n/mine — Start mining\n/help — Help"
        );
      }

      return new Response("OK");

    } catch (error) {
      console.error("ERROR:", error);
      return new Response("Internal error", { status: 500 });
    }
  }
};

async function sendTelegramMessage(token, chatId, text) {
  const response = await fetch(
    `https://api.telegram.org/bot${token}/sendMessage`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        chat_id: chatId,
        text: text
      })
    }
  );

  const result = await response.text();

  console.log("Telegram response:", result);

  if (!response.ok) {
    throw new Error(`Telegram API ${response.status}: ${result}`);
  }
}

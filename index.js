export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("NexaMine is online 🚀");
    }

    try {
      const update = await request.json();

      if (update.message) {
        const chatId = update.message.chat.id;
        const text = update.message.text || "";
        const name = update.message.from?.first_name || "Miner";

        if (text === "/start") {
          await sendMessage(
            chatId,
            `⛏️ Welcome to NexaMine, ${name}!\n\n` +
            `Mine • Earn • Grow 🚀\n\n` +
            `🪙 Balance: 0 NXM\n` +
            `⛏️ Mining: Ready\n\n` +
            `Use /mine to start mining.`
          );
        }

        if (text === "/mine") {
          await sendMessage(
            chatId,
            "⛏️ Mining started!\n\n🪙 +10 NXM\n\n" +
            "NexaMine test mining is active."
          );
        }

        if (text === "/help") {
          await sendMessage(
            chatId,
            "NexaMine Commands:\n\n" +
            "/start — Open NexaMine\n" +
            "/mine — Start mining\n" +
            "/help — Help"
          );
        }
      }

      return new Response("OK");
    } catch (error) {
      return new Response("Error", { status: 500 });
    }

    async function sendMessage(chatId, text) {
      await fetch(
        `https://api.telegram.org/bot${env.BOT_TOKEN}/sendMessage`,
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
    }
  }
};

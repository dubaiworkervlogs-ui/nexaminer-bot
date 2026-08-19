export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("NexaMine is online 🚀");
    }

    try {
      const update = await request.json();

      if (!update.message) {
        return new Response("OK");
      }

      const chatId = update.message.chat.id;
      const user = update.message.from;
      const text = update.message.text || "";

      // Create user if they don't exist
      await env.DB.prepare(`
        INSERT OR IGNORE INTO users
        (telegram_id, username, first_name, balance, mining_rate, last_mined, created_at)
        VALUES (?, ?, ?, 0, 10, 0, ?)
      `)
        .bind(
          String(user.id),
          user.username || null,
          user.first_name || null,
          Math.floor(Date.now() / 1000)
        )
        .run();

      // START
      if (text === "/start") {
        const result = await env.DB.prepare(`
          SELECT balance, mining_rate FROM users
          WHERE telegram_id = ?
        `)
          .bind(String(user.id))
          .first();

        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          `⛏️ Welcome to NexaMine!\n\n` +
          `Mine • Earn • Grow 🚀\n\n` +
          `🪙 Balance: ${result.balance} NXM\n` +
          `⛏️ Mining Rate: ${result.mining_rate} NXM\n\n` +
          `Use /mine to mine.`
        );
      }

      // MINE
      else if (text === "/mine") {
        const result = await env.DB.prepare(`
          SELECT balance, mining_rate
          FROM users
          WHERE telegram_id = ?
        `)
          .bind(String(user.id))
          .first();

        const newBalance = result.balance + result.mining_rate;

        await env.DB.prepare(`
          UPDATE users
          SET balance = ?, last_mined = ?
          WHERE telegram_id = ?
        `)
          .bind(
            newBalance,
            Math.floor(Date.now() / 1000),
            String(user.id)
          )
          .run();

        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          `⛏️ Mining successful!\n\n` +
          `🪙 +${result.mining_rate} NXM\n` +
          `💰 Balance: ${newBalance} NXM`
        );
      }

      // HELP
      else if (text === "/help") {
        await sendTelegramMessage(
          env.BOT_TOKEN,
          chatId,
          `⛏️ NexaMine Help\n\n` +
          `/start — Open NexaMine\n` +
          `/mine — Mine coins\n` +
          `/help — Help`
        );
      }

      return new Response("OK");

    } catch (error) {
      console.error("NexaMine Error:", error);
      return new Response("Internal Server Error", { status: 500 });
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

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error);
  }
}

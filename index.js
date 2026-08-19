export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // Open the game website
    if (request.method === "GET") {
      if (env.ASSETS) {
        return env.ASSETS.fetch(request);
      }

      return new Response("NexaMine is online 🚀");
    }

    // Telegram webhook
    if (request.method === "POST") {
      try {
        const update = await request.json();

        if (!update.message) {
          return new Response("OK");
        }

        const chatId = update.message.chat.id;
        const user = update.message.from;
        const text = update.message.text || "";

        // Create user
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
          await sendTelegramMessage(
            env.BOT_TOKEN,
            chatId,
            "⛏️ Welcome to NexaMine!\n\nMine • Earn • Grow 🚀",
            {
              inline_keyboard: [
                [
                  {
                    text: "⛏️ PLAY NEXAMINE",
                    web_app: {
                      url: "https://nexaminer-bot.dubaiworkervlogs.workers.dev/"
                    }
                  }
                ]
              ]
            }
          );
        }

        // Old command kept for testing
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
            `⛏️ Mining successful!\n\n+${result.mining_rate} NXM\n💰 Balance: ${newBalance} NXM`
          );
        }

        return new Response("OK");

      } catch (error) {
        console.error("NexaMine Error:", error);
        return new Response("Internal Server Error", {
          status: 500
        });
      }
    }

    return new Response("NexaMine");
  }
};

async function sendTelegramMessage(
  token,
  chatId,
  text,
  replyMarkup = null
) {
  const body = {
    chat_id: chatId,
    text: text
  };

  if (replyMarkup) {
    body.reply_markup = replyMarkup;
  }

  const response = await fetch(
    `https://api.telegram.org/bot${token}/sendMessage`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(body)
    }
  );

  const result = await response.text();

  console.log("Telegram response:", result);

  if (!response.ok) {
    throw new Error(result);
  }
}

// Note: This file only contains the webhook code used to trigger deployment on EC2.
// It is **not part of the project production source code** and is only stored here for backup and version tracking purposes.
// The webhook service actually runs on the EC2 server.

const http = require("http");
const { exec } = require("child_process");

const PORT = 5001;
const DEPLOY_SCRIPT = "/home/ec2-user/ELEC5620_AI_Trip_Assistant/deploy.sh";

const server = http.createServer((req, res) => {
    if (req.method !== "POST" || req.url !== "/webhook") {
        res.writeHead(404);
        return res.end("Not Found");
    }

    let body = "";
    req.on("data", chunk => {
        body += chunk.toString();
    });

    req.on("end", () => {
        try {
            const payload = JSON.parse(body);
            const branch = payload.ref?.split("/")?.pop() || "unknown";
            const eventType = req.headers["x-github-event"];
            const deliveryId = req.headers["x-github-delivery"];

            console.log(`🔔 Webhook Trigger Received`);
            console.log(`📌 Event: ${eventType}`);
            console.log(`📌 Branch: ${branch}`);
            console.log(`📌 Delivery ID: ${deliveryId}`);

            if (eventType === "push" && branch === "main") {
                console.log("🚀 Trigger valid → Deploying...");

                exec(`chmod +x ${DEPLOY_SCRIPT} && sh ${DEPLOY_SCRIPT}`, (error, stdout, stderr) => {
                    if (error) console.error(`❌ Error: ${error.message}`);
                    if (stderr) console.error(`⚠️ Warning: ${stderr}`);
                    console.log(`✅ Deploy Output:\n${stdout}`);
                });

                res.writeHead(200);
                return res.end("Deploy started ✅");
            }

            console.log("ℹ️ Event Ignored (Not main)");
            res.writeHead(200);
            res.end("Ignored ✅");

        } catch (err) {
            console.error("❌ JSON Parse Error:", err.message);
            res.writeHead(400);
            res.end("Invalid JSON ❌");
        }
    });
});

// ✅ Listen from external network
server.listen(PORT, "0.0.0.0", () =>
    console.log(`✅ Webhook Server running on port ${PORT}`)
);
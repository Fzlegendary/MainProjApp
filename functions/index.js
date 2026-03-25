const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();

exports.sendChatNotification = functions.database
    .ref("/chats/{chatId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const chatId = context.params.chatId;
        const messageId = context.params.messageId;

        const messageData = snapshot.val();
        if (!messageData) {
            return null;
        }

        const senderUsername = messageData.senderUsername;
        const messageText = messageData.text;
        const messageTime = messageData.time;

        const chatSnapshot = await db.ref(`chats/${chatId}`).once("value");
        const chatData = chatSnapshot.val();

        if (!chatData || !chatData.participants) {
            return null;
        }

        const participants = chatData.participants;
        let recipientEmail = null;

        for (const [email, value] of Object.entries(participants)) {
            if (email !== senderUsername && value === true) {
                recipientEmail = email;
                break;
            }
        }

        if (!recipientEmail) {
            const allParticipants = Object.keys(participants);
            const currentUserEmail = await getUserEmailByUsername(senderUsername);
            
            for (const email of allParticipants) {
                if (email !== currentUserEmail) {
                    recipientEmail = email;
                    break;
                }
            }
        }

        if (!recipientEmail) {
            console.log("Could not determine recipient");
            return null;
        }

        const recipientSafeEmail = recipientEmail.replace(/\./g, "_");
        
        const userSnapshot = await db.ref(`users/${recipientSafeEmail}`).once("value");
        const userData = userSnapshot.val();

        if (!userData) {
            console.log("Recipient user data not found");
            return null;
        }

        const recipientToken = userData.fcmToken;

        if (!recipientToken) {
            console.log("Recipient FCM token not found");
            return null;
        }

        const payload = {
            notification: {
                title: `New message from ${senderUsername}`,
                body: messageText
            },
            data: {
                chatId: chatId,
                senderUsername: senderUsername,
                messageText: messageText,
                messageTime: messageTime || "",
                messageKey: messageId
            }
        };

        try {
            await admin.messaging().send({
                token: recipientToken,
                notification: payload.notification,
                data: payload.data,
                android: {
                    priority: "high",
                    notification: {
                        channelId: "messages_channel",
                        priority: "high"
                    }
                }
            });
            console.log("Notification sent successfully to", recipientEmail);
            return null;
        } catch (error) {
            console.error("Error sending notification:", error);
            if (error.code === "messaging/invalid-recipient-token" ||
                error.code === "messaging/registration-token-not-registered") {
                await db.ref(`users/${recipientSafeEmail}/fcmToken`).remove();
            }
            return null;
        }
    });

async function getUserEmailByUsername(username) {
    const usersSnapshot = await db.ref("users")
        .orderByChild("username")
        .equalTo(username)
        .once("value");
    
    if (usersSnapshot.exists()) {
        const users = usersSnapshot.val();
        for (const email in users) {
            return email.replace(/_/g, ".");
        }
    }
    return null;
}

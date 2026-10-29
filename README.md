<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<title>Jarvis AI Assistant</title>

<style>
* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #07111f;
    color: white;
    text-align: center;
}

header {
    padding: 22px;
    background: #0c1b2e;
    color: #00eaff;
    font-size: 25px;
    font-weight: bold;
}

.container {
    max-width: 600px;
    margin: auto;
    padding: 20px;
}

.circle {
    width: 145px;
    height: 145px;
    border: 3px solid #00eaff;
    border-radius: 50%;
    margin: 25px auto;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 60px;
    box-shadow: 0 0 25px #00eaff;
}

#status {
    color: #00eaff;
    margin: 15px;
}

#chat {
    height: 300px;
    overflow-y: auto;
    background: #102238;
    border-radius: 12px;
    padding: 12px;
    text-align: left;
    margin-bottom: 15px;
}

.message {
    padding: 10px;
    margin: 8px 0;
    border-radius: 8px;
    overflow-wrap: anywhere;
}

.user {
    background: #164b70;
}

.bot {
    background: #20344b;
}

input {
    width: 68%;
    padding: 13px;
    border-radius: 8px;
    border: none;
    outline: none;
}

button {
    padding: 12px;
    border: none;
    border-radius: 8px;
    background: #00d9ff;
    color: #00111b;
    font-weight: bold;
    margin: 4px;
    cursor: pointer;
}

button:disabled {
    opacity: 0.5;
}

.small {
    font-size: 12px;
    color: #b8c9d9;
}
</style>
</head>

<body>

<header>🤖 JARVIS AI</header>

<div class="container">

    <div class="circle">◉</div>

    <h2>Your Personal AI Assistant</h2>

    <p id="status">Assistant Ready</p>

    <div id="chat">
        <div class="message bot">
            Jarvis: Namaste! Main aapka AI assistant hoon.
            Mujhse kuch bhi pooch sakte ho.
        </div>
    </div>

    <input
        type="text"
        id="question"
        placeholder="Yahan sawaal likho..."
    >

    <button id="send" onclick="askAI()">Send</button>

    <br>

    <button onclick="startVoice()">🎙️ Speak</button>

    <button onclick="stopVoice()">🔇 Stop Voice</button>

    <p class="small">
        AI replies require an API connection.
        Microphone support depends on your browser.
    </p>

</div>

<script>
const chat = document.getElementById("chat");
const statusText = document.getElementById("status");
const questionInput = document.getElementById("question");
const sendButton = document.getElementById("send");

function addMessage(sender, text, className) {
    const div = document.createElement("div");

    div.className = "message " + className;
    div.textContent = sender + ": " + text;

    chat.appendChild(div);
    chat.scrollTop = chat.scrollHeight;
}

async function askAI() {
    const question = questionInput.value.trim();

    if (!question) {
        alert("Pehle apna sawaal likho!");
        return;
    }

    addMessage("You", question, "user");

    questionInput.value = "";
    sendButton.disabled = true;
    statusText.textContent = "AI soch raha hai...";

    try {
        // API connection yahan add karna hai.
        // API key ko browser ke code mein mat rakhna.

        const response = await fetch("/api/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                message: question
            })
        });

        if (!response.ok) {
            throw new Error("API connection nahi hua.");
        }

        const data = await response.json();

        const answer = data.reply || "Jawab nahi mila.";

        addMessage("Jarvis", answer, "bot");

        speak(answer);

        statusText.textContent = "Assistant Ready";

    } catch (error) {
        addMessage(
            "Jarvis",
            "AI server connect nahi hai. Backend aur AI API setup karna baaki hai.",
            "bot"
        );

        statusText.textContent = "Connection Required";

    } finally {
        sendButton.disabled = false;
    }
}

questionInput.addEventListener("keydown", function(event) {
    if (event.key === "Enter") {
        askAI();
    }
});

function speak(text) {
    if (!("speechSynthesis" in window)) {
        statusText.textContent = "Voice support available nahi hai.";
        return;
    }

    window.speechSynthesis.cancel();

    const speech = new SpeechSynthesisUtterance(text);
    speech.lang = "hi-IN";
    speech.rate = 1;

    window.speechSynthesis.speak(speech);
}

function startVoice() {
    const Recognition =
        window.SpeechRecognition ||
        window.webkitSpeechRecognition;

    if (!Recognition) {
        alert("Voice recognition ke liye supported browser use karo.");
        return;
    }

    const recognition = new Recognition();

    recognition.lang = "hi-IN";
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = function() {
        statusText.textContent = "Sun raha hoon...";
    };

    recognition.onresult = function(event) {
        questionInput.value =
            event.results[0][0].transcript;

        askAI();
    };

    recognition.onerror = function() {
        statusText.textContent = "Microphone error";
    };

    recognition.onend = function() {
        if (statusText.textContent === "Sun raha hoon...") {
            statusText.textContent = "Assistant Ready";
        }
    };

    recognition.start();
}

function stopVoice() {
    if ("speechSynthesis" in window) {
        window.speechSynthesis.cancel();
    }

    statusText.textContent = "Voice Stopped";
}
</script>

</body>
</html>

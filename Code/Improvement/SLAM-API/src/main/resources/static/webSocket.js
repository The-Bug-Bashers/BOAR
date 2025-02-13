const socketURL = 'wss://ws.ifelse.io';
const socket = new WebSocket(socketURL);

function showMessage(message, received) {
    const container = document.getElementById("consoleOutput");
    const messageElement = document.createElement("div");
    messageElement.textContent = message;
    messageElement.classList.add("message");

    if (received) {
        messageElement.classList.add("receivedMessage");
    } else {
        messageElement.classList.add("sentMessage");
    }

    container.appendChild(messageElement);

    console.log('Message received from server:', message);
}

function sendMessage(message) {
    console.log(`Sending message: ${message}`);
    socket.send(message);

    showMessage(message, false);

    document.getElementById("consoleInput").value = "";
}

// Event listener for when the connection is open
socket.addEventListener('open', () => {
    console.log('WebSocket connection established.');

    // Send a message to the WebSocket server
    const messageToSend = "Hello, WebSocket!";
    sendMessage(messageToSend);
});

// Event listener for when a message is received
socket.addEventListener('message', (event) => {
    showMessage(event.data, true);
});

// Event listener for when the connection is closed
socket.addEventListener('close', () => {
    console.log('WebSocket connection closed.');
});

// Event listener for errors
socket.addEventListener('error', (error) => {
    console.error('WebSocket error:', error);
});

const roomPositions = {
    kitchen: { x: 540, y: 200 },
    dining: { x: 380, y: 360 },
    master: { x: 150, y: 220 },
    bedroom2: { x: 380, y: 550 },
    bathroom: { x: 620, y: 550 }
};

function drawPath() {
    const start = document.getElementById("startRoom").value;
    const end = document.getElementById("endRoom").value;

    const canvas = document.getElementById("path");
    const ctx = canvas.getContext("2d");

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const s = roomPositions[start];
    const e = roomPositions[end];

    ctx.lineWidth = 5;
    ctx.strokeStyle = "red";

    ctx.beginPath();
    ctx.moveTo(s.x, s.y);
    ctx.lineTo(e.x, e.y);
    ctx.stroke();
}

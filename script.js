const roomPositions = {
    kitchen: { x: 480, y: 70 },
    living: { x: 220, y: 170 },
    bedroom: { x: 90, y: 270 },
    bathroom: { x: 480, y: 270 }
};

function drawPath() {
    const start = document.getElementById("startRoom").value;
    const end = document.getElementById("endRoom").value;

    const canvas = document.getElementById("path");
    const ctx = canvas.getContext("2d");

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const s = roomPositions[start];
    const e = roomPositions[end];

    ctx.lineWidth = 4;
    ctx.strokeStyle = "red";

    ctx.beginPath();
    ctx.moveTo(s.x, s.y);
    ctx.lineTo(e.x, e.y);
    ctx.stroke();
}
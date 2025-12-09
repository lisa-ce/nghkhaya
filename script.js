const roomPositions = {
    kitchen: { x: 540, y: 200 },
    dining: { x: 380, y: 360 },
    master: { x: 150, y: 220 },
    bedroom2: { x: 380, y: 550 },
    bathroom: { x: 620, y: 550 }
};
const nodes = {
  masterDoor: { x: 260, y: 300 },
  kitchenCenter: { x: 380, y: 300 },
  diningDoor: { x: 380, y: 390 },
  bedroom2Door: { x: 330, y: 450 },
  bathroomDoor: { x: 550, y: 450 }
};
const edges = {
  masterDoor: ["kitchenCenter"],
  kitchenCenter: ["masterDoor", "diningDoor", "bedroom2Door"],
  diningDoor: ["kitchenCenter"],
  bedroom2Door: ["kitchenCenter", "bathroomDoor"],
  bathroomDoor: ["bedroom2Door"]
};
const roomToNode = {
    master: "masterDoor",
    kitchen: "kitchenCenter",
    dining: "diningDoor",
    bedroom2: "bedroom2Door",
    bathroom: "bathroomDoor"
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

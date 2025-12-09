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
function findPath(start, end) {
    let queue = [start];
    let visited = {};
    visited[start] = null;

    while (queue.length > 0) {
        let current = queue.shift();

        if (current === end) break;

        for (let neighbor of edges[current]) {
            if (!visited.hasOwnProperty(neighbor)) {
                visited[neighbor] = current;
                queue.push(neighbor);
            }
        }
    }

    // Reconstruct path
    let path = [];
    let step = end;

    while (step !== null) {
        path.unshift(step);
        step = visited[step];
    }

    return path;
}

function drawPath() {
    const startRoom = document.getElementById("startRoom").value;
    const endRoom = document.getElementById("endRoom").value;

    const startNode = roomToNode[startRoom];
    const endNode = roomToNode[endRoom];

    const pathNodes = findPath(startNode, endNode);

    const canvas = document.getElementById("path");
    const ctx = canvas.getContext("2d");

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.lineWidth = 5;
    ctx.strokeStyle = "red";

    ctx.beginPath();

    // Move to first node
    let first = nodes[pathNodes[0]];
    ctx.moveTo(first.x, first.y);

    // Draw along each step
    for (let i = 1; i < pathNodes.length; i++) {
        let p = nodes[pathNodes[i]];
        ctx.lineTo(p.x, p.y);
    }

    ctx.stroke();
}


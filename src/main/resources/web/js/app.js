function onload() {

    let members = new Map();

    function handleEvent(event) {
        if (event.data !== "") {
            let clusterEvent = JSON.parse(event.data);
            console.log(clusterEvent);
            if (clusterEvent.status === "removed") {
                members.delete(clusterEvent.label);
            } else {
                members.set(clusterEvent.label, clusterEvent.status);
            }
            updateCircles(members);
        }
    }

    let events = new EventSource('events');
    events.addEventListener("message", handleEvent, false);
}

function updateCircles(members) {
    let circles = d3.select("svg").selectAll("circle").data(Array.from(members));

    circles
        .style("fill", ([label, status], n) => statusToColor(status, n));

    circles.enter()
        .append("circle")
        .attr("r", 20)
        .attr("cy", 60)
        .attr("cx", (member, n) => n * 100 + 30)
        .style("fill", ([label, status], n) => statusToColor(status, n));

    circles.exit()
        .remove();
}

function statusToColor(status, n) {
    console.log(n + ": " + status);
    return status === "joining" ? "lightblue"
        : status === "weakly-up" ? "lightgreen"
            : status === "up" ? "green"
                : status === "leaving" ? "yellow"
                    : status === "exiting" ? "lightyellow"
                        : status === "down" ? "orange"
                            : status === "unreachable" ? "red"
                                : status === "reachable" ? "green"
                                    : "gray";
}

function onload() {
    let selectedMember = d3.select("#selectedMember");
    let members = new Map();

    function handleEvent(event) {
        if (event.data !== "") {
            let {address, status, roles} = JSON.parse(event.data);
            if (status === "removed") {
                members.delete(address);
            } else {
                let m = {status: status, isStatic: roles.includes('static')};
                members.set(address, m);
            }
            updateCircles(selectedMember, members);
        }
    }

    let events = new EventSource('events');
    events.addEventListener("message", handleEvent, false);
}

function updateCircles(selectedMember, members) {
    let circles = d3.select("svg").selectAll("circle").data(Array.from(members));

    circles
        .style("fill", ([, {status}]) => statusToColor(status));

    circles.enter()
        .append("circle")
        .attr("r", 20)
        .attr("cy", 60)
        .attr("cx", (member, n) => n * 100 + 30)
        .style("stroke", "orange")
        .style("fill", ([, {status}]) => statusToColor(status))
        .style("stroke-width", ([, {isStatic}]) => isStatic ? 2 : 0)
        .on("click", ([address,]) => selectedMember.text(address));

    circles.exit()
        .remove();
}

function statusToColor(status) {
    console.log("status", status);
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

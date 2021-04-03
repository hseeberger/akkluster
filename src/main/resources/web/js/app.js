function onload() {
    const selectedMember = d3.select("#selectedMember");
    const members = new Map();

    function handleEvent(event) {
        if (event.data !== "") {
            let {address, status, roles} = JSON.parse(event.data);
            let isStatic = roles.includes('static');
            if (status === "removed") {
                members.delete(address);
            } else {
                let m = {status: status, isStatic: isStatic};
                members.set(address, m);
            }
            updateCircles(selectedMember, members);
        }
    }

    const events = new EventSource('events');
    events.addEventListener("message", handleEvent, false);
}

function updateCircles(selectedMember, members) {
    d3.select("svg").selectAll("circle")
        .data(Array.from(members))
        .join(
            enter => enter.append("circle"),
            update => update,
            exit => exit.remove()
        )
        .attr("r", 20)
        .attr("cx", (_, n) => n * 100 + 30)
        .attr("cy", 60)
        .style("fill", ([, {status}]) => statusToColor(status))
        .style("stroke", "orange")
        .style("stroke-width", ([, {isStatic}]) => isStatic ? 2 : 0)
        .on("click", (_, [address,]) => selectedMember.text(address));
}

function statusToColor(status) {
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

function onload() {
    let selectedMember = d3.select("#selectedMember");
    let members = new Map();

    function handleEvent(event) {
        if (event.data !== "") {
            let {address, status} = JSON.parse(event.data);
            console.log(address + " is " + status);
            if (status === "removed") {
                members.delete(address);
            } else {
                members.set(address, status);
            }
            updateCircles(selectedMember, members);
        }
    }

    let events = new EventSource('events');
    events.addEventListener("message", handleEvent, false);
}

function updateCircles(selectedMember, members) {
    let circles = d3.select("svg").selectAll("circle").data(Array.from(members));

    circles.style("fill", ([, status], n) => statusToColor(status, n));

    circles.enter()
        .append("circle")
        .attr("r", 20)
        .attr("cy", 60)
        .attr("cx", (member, n) => n * 100 + 30)
        .style("fill", ([, status]) => statusToColor(status))
        .on("click", ([address,]) => selectedMember.text(address));

    circles.exit()
        .remove();
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

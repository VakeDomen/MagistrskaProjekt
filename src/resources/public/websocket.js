// small helper function for selecting element by id
let id = id => document.getElementById(id);
var config = {
    springLength : 10,
    springCoeff :  1e-7,
    dragCoeff : 0.03,
    timestep : 28,
    pause_rendering : !1,
    gravity : -0.2
}

var graph = Viva.Graph.graph();
var layout = Viva.Graph.Layout.forceDirected(graph, config);
var renderer = Viva.Graph.View.renderer(graph, {
    layout : layout
});

renderer.run();
var current_color=0;
colors = [
    "#6a388c",
    "#1fad43",
    "#8c0934",
    "#095b10",
    "#3bbef0",
    "#fe8713",
    "#2bdfcd",
    "#f2e401",
    "#8c01ab",
    "#d43d54",
    "#c6f701",
    "#6930f6"
];

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/update");
ws.onmessage = msg => updateGraph(msg);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
id("start").addEventListener("click", () => sendAndClear("start"));

function sendAndClear(message) {
    if (message !== "") {
        ws.send(message);
    }

}

function updateGraph(msg) { // Update chat-panel and list of connected users
    let data = JSON.parse(msg.data);
    //console.log("Received message:", data.action)
    switch (data.action){
    case "new_node":
        graph.addNode(data.node_id);
    break;
    case "new_connection" :
        graph.addLink(data.source, data.destination);
    break;
    case "message_sent" :
        graph.addLink(data.source, data.destination);
    break;
    case "message_not_sent" :
        const o = graph.getNode(data.source);
        for (const link of o.links) {
            if (link.toId == data.destination) {
                graph.removeLink(link);
                renderer.getGraphics().getNodeUI(data.destination).attr("fill", colors[9]);
                break;
            }
        }
    break;
    case "new_message_generated" :
        renderer.getGraphics().getNodeUI(data.nodeId).attr("fill", colors[10]);
    break;
    case "reset_graph" :
        graph.clear();
    break;
    case "sim_successful" :
        console.log(data);
        var line = "-> <span class='key'> Simulation " +  data.simNumber + " successfully completed!</span>"
        id("right").innerHTML+= line;
    break;
    case "sim_failed" :
        console.log(data);
        var line = "-> <br><span class='key'> Simulation " +  data.simNumber + " failed! Number of uninformed nodes: " + data.uninformed + "</span>"
            id("right").innerHTML+= line;
    break;
    case "deactivate_node":
        renderer.getGraphics().getNodeUI(data.nodeId).attr("fill", colors[9]);
    break;
    case "message_received" :
        const origin = graph.getNode(data.source);
        for (const link of origin.links) {
            if (link.toId == data.destination) {
                graph.removeLink(link);
                renderer.getGraphics().getNodeUI(data.destination).attr("fill", colors[10]);
                break;
            }
        }
    break;
    case "new_block_received":
        if(current_block != data.block){
                current_block = data.block;
                var line = "-> <span class='key'> New block produced by node</span> : <span class ='value'>"
                + data.node_id.substring(0,5) + "</span> <span class='key'> At height </span> : <span class ='value'>"+current_block+"</span></br>";
                //line.style.color = colors[data.current_block % colors.length];
                id("right").innerHTML+= line;
                //simulating propagation
                renderer.getGraphics().getNodeUI(data.node_id).attr("fill",colors[data.block% colors.length]);
         }else{
             renderer.getGraphics().getNodeUI(data.node_id).attr("fill",colors[data.block% colors.length]);
         }
    break;
    case "simulation":
           var line = "-> <span class='key'> Simulation for block</span> : <span class ='value'>"
                    + data.block + " complete</span> <span class='key'> Propagation time </span> : <span class ='value'>"
                    + (data.end - data.start)
                    + " ms</span></br>";
            id("left").innerHTML+= line;
    break;
    }
}
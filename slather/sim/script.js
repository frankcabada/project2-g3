function parse_float(x)
{
    if (isNaN(parseFloat(x)) || !isFinite(x))
	throw "Invalid number: " + x;
    return +x;
}

function parse_integer(x)
{
    x = parse_float(x);
    if (Math.round(x) != x)
	throw "Invalid integer: " + x;
    return Math.round(x);
}

function process(data)
{
    // check the canvas size
    var canvas = document.getElementById("canvas");
    var colors = ["red", "green", "blue", "purple", "orange", "cyan", "yellow", "hotpink", "palegreen", "maroon"];
    var y_base = 60;
    var size = canvas.height - y_base;
    var xoffset = 30;    
    var x_base = (canvas.width - size) * 0.5;
    // parse the data
    data = data.split("\n");
    if (data.length != 4)
	throw "Invalid data format" + data.length;
    
    params = data[0].split(",");
    var side = parse_integer(params[0]);
    var refresh = parse_integer(params[1]);

    // clear the canvas
    var ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    // draw the room
    ctx.beginPath();
    ctx.moveTo(x_base + xoffset,        y_base);
    ctx.lineTo(x_base + size + xoffset, y_base);
    ctx.lineTo(x_base + size + xoffset, y_base + size);
    ctx.lineTo(x_base + xoffset,        y_base + size);
    ctx.lineTo(x_base + xoffset,        y_base);
/*    ctx.moveTo(x_base + 1.5,        y_base + 1.5);
    ctx.lineTo(x_base + size - 1.5, y_base + 1.5);
    ctx.lineTo(x_base + size - 1.5, y_base + size - 1.5);
    ctx.lineTo(x_base + 1.5,        y_base + size - 1.5);
    ctx.lineTo(x_base + 1.5,        y_base + 1.5);*/
    ctx.lineWidth = 3;
    ctx.strokeStyle = "black";
    ctx.stroke();

    // parse the scores
    ctx.font = "18px Arial";
    ctx.textAlign = "left";
    ctx.lineWidth = 4;
    ctx.strokeStyle = "darkgrey";
    scores = data[1].split(";");
    for (var i=0; i<scores.length; i++) {
	coord = scores[i].split(",");
	g = coord[0];
	score = parse_float(coord[1]);
	ctx.strokeText(g + " :  " + score, 0, y_base + 30*i + 30);
	ctx.fillStyle = colors[i];
	ctx.fillText(g + " :  " + score, 0, y_base + 30*i + 30);
    }
    
    // parse the cells
    cells = data[2].split(";");    
    for (var i=0; i<cells.length; i++) {
	coord = cells[i].split(",");
	g = parse_integer(coord[0]);
	x = (parse_float(coord[1]) / side) * size + x_base + xoffset;
	y = (parse_float(coord[2]) / side) * size + y_base;
	d = (parse_float(coord[3]) / side) * size;
	ctx.beginPath();
	ctx.arc(x,y,d/2,0,2.0*Math.PI);
	ctx.fillStyle = colors[g];
	ctx.fill();
	ctx.stroke();
    }

    // parse the pheromes
    pheromes = data[3].split(";");
    if (pheromes.length > 1) {
	for (var i=0; i<pheromes.length; i++) {
	    coord = pheromes[i].split(",");
	    g = parse_integer(coord[0]);
	    x = (parse_float(coord[1]) / side) * size + x_base + xoffset;
	    y = (parse_float(coord[2]) / side) * size + y_base;
	    ctx.beginPath();
	    ctx.arc(x,y,0.5,0,2.0*Math.PI);
	    ctx.fillStyle = colors[g];
	    ctx.fill();
	}
    }
    return refresh;
 }

var latest_version = -1;

function ajax(version, retries, timeout)
{
    var xhr = new XMLHttpRequest();
    xhr.onload = (function() {
	var refresh = -1;
	try {
	    if (xhr.readyState != 4)
		throw "Incomplete HTTP request: " + xhr.readyState;
	    if (xhr.status != 200)
		throw "Invalid HTTP status: " + xhr.status;
	    refresh = process(xhr.responseText);
	    if (latest_version < version)
		latest_version = version;
	    else
		refresh = -1;
	} catch (message) { alert(message); }
	if (refresh >= 0)
	    setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
    });
    xhr.onabort   = (function() { location.reload(true); });
    xhr.onerror   = (function() { location.reload(true); });
    xhr.ontimeout = (function() {
	if (version <= latest_version)
	    console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
	else if (retries == 0)
	    location.reload(true);
	else {
	    console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
	    ajax(version, retries - 1, timeout * 2);
	}
    });
    xhr.open("GET", "data.txt", true);
    xhr.responseType = "text";
    xhr.timeout = timeout;
    xhr.send();
}

ajax(0, 10, 100);

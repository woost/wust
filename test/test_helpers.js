var querystring = require('querystring');
var http = require('http');

function clearDatabase(after) {
    var data = querystring.stringify({ statements:[ {
        statement: "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r",
        resultDataContents:[]
    }]});

    var options = {
        host: 'localhost',
        port: 6474,
        path: '/db/data/transaction/commit',
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(data)
        }
    };

    var req = http.request(options, function(res) {
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            console.log("body: " + chunk);
            after();
        });
    });

    console.log("write");
    req.write(data);
    console.log("end");
    req.end();
}

clearDatabase(function() {console.log("juhu")});

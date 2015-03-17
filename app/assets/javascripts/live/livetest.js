// We are now ready to cut the request
var request = { url: document.location.toString() + "",
    contentType : "application/json",
    trackMessageLength : true,
    shared : true,
    transport : "websocket" ,
    fallbackTransport: "long-polling"};

request.onOpen = function(response) {
    console.log("Atmosphere connected using " + response.transport );
};

request.onTransportFailure = function(errorMsg, request) {
    console.log(errorMsg);
};

request.onMessage = function (response) {
    console.log(response);
};

request.onClose = function(response) {
    console.log("Atmosphere disconnected");
};
console.log("subscribing...");
atmosphere.subscribe(request);

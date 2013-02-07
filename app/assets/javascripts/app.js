window.ZenChat = (function(){
  "use strict";

  var ZC = {
    input: null,
    socket: null
  };

  ZC.initAjaxPost = function() {
    ZC.input = $("#input");
    $("#input-form").submit(function(e){
      e.preventDefault();
      ZC.send({
        "message": ZC.input.val()
      });
      ZC.input.val("");
    });
  };

  ZC.send = function(msg) {
    ZC.socket.send(JSON.stringify(msg));
  }

  ZC.initSocket = function(roomName, socket) {
    ZC.roomName = roomName;
    ZC.socket = socket;
    ZC.socket.onmessage = function(event) {
      var data = JSON.parse(event.data);
      var date = new Date(data.date);
      var fragment = '<article class="message" data-id="' + data.id + '">' +
         '<header>' +
           '<div class="nickname">'+data.author.name+'</div>' +
           '<div class="avatar"><img src="'+data.author.picture+'" class="img-rounded" alt="'+data.author.name+'" /></div>' +
         '</header>' +
         '<section>' +
           '<p>'+data.text+'</p>' +
         '</section>' +
         '<footer>' +
           '<div class="time">'+date.getHours()+':'+date.getMinutes()+'</div>' +
         '</footer>' +
       '</article>';
      $("#messages").append(fragment);
    };
    ZC.socket.onopen = function(event) {};
    ZC.socket.onclose = function(event) {};
    ZC.socket.onerror = function(event) {};
  };

  ZC.init = function(){};
  return ZC;
})();

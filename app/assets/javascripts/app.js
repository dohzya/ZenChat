window.ZenChat = (function($){
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

  ZC.displayMessage = function(msg) {
    var date = new Date(msg.date);
    var fragment = '<article class="message" data-id="' + msg.id + '">' +
       '<header>' +
         '<div class="nickname">'+msg.author.name+'</div>' +
         '<div class="avatar"><img src="'+msg.author.picture+'" class="img-rounded" alt="'+msg.author.name+'" /></div>' +
       '</header>' +
       '<section>' +
         '<p>'+msg.text+'</p>' +
       '</section>' +
       '<footer>' +
         '<div class="time">'+date.getHours()+':'+date.getMinutes()+'</div>' +
       '</footer>' +
     '</article>';
    $("#messages").append(fragment);
  };

  ZC.send = function(msg) {
    ZC.socket.send(JSON.stringify(msg));
  }

  ZC.initSocket = function(roomName, socket) {
    ZC.roomName = roomName;
    ZC.socket = socket;
    ZC.socket.onmessage = function(event) {
      var msg = JSON.parse(event.data);
      ZC.displayMessage(msg);
    };
    ZC.socket.onopen = function(event) {};
    ZC.socket.onclose = function(event) {};
    ZC.socket.onerror = function(event) {};
  };

  ZC.init = function(){};
  return ZC;
})(jQuery);

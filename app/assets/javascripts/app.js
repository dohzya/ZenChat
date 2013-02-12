window.ZenChat = (function($, markdown){
  "use strict";

  var input = null,
      socket = null;

  function initAjaxPost() {
    input = $("#input");
    $("#input-form").submit(function(e){
      e.preventDefault();
      send({
        "message": input.val()
      });
      input.val("");
    });
    scrollToBottom();
  }

  function scrollToBottom() {
    window.scrollTo(0, $("body").height());
    window.scrollBy(0, 1000);
  }

  function scrollIfNeeded(f) {
    var need = window.scrollY + $(window).height() - $("body").height() > 0;
    f();
    if (need) scrollToBottom();
  }

  function displayMessage(msg) {
    var date = new Date(msg.date);
    var text = markdown.toHTML(msg.text.replace(/^#/, ' #'));
    var fragment = '<article class="message" data-id="' + msg.id + '">' +
       '<header>' +
         '<div class="nickname">'+msg.author.name+'</div>' +
         '<div class="avatar"><img src="'+msg.author.picture+'" class="img-rounded" alt="'+msg.author.name+'" /></div>' +
       '</header>' +
       '<section>' +
         '<p>'+text+'</p>' +
       '</section>' +
       '<footer>' +
         '<div class="time">'+date.getHours()+':'+date.getMinutes()+'</div>' +
       '</footer>' +
     '</article>';
    scrollIfNeeded(function(){
      $("#messages").append(fragment);
    });
  }

  function send(msg) {
    socket.send(JSON.stringify(msg));
  }

  function initSocket(roomName, socket) {
    roomName = roomName;
    socket = socket;
    socket.onmessage = function(event) {
      var msg = JSON.parse(event.data);
      displayMessage(msg);
    };
    socket.onopen = function(event) {};
    socket.onclose = function(event) {};
    socket.onerror = function(event) {};
  }

  function init(){}

  return {
    initAjaxPost: initAjaxPost,
    scrollToBottom: scrollToBottom,
    scrollIfNeeded: scrollIfNeeded,
    displayMessage: displayMessage,
    send: send,
    initSocket: initSocket,
    init: init
  };
})(jQuery, markdown);

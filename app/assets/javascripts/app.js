window.ZenChat = (function($, markdown){
  "use strict";

  var input = null,
      socket = null,
      notifications = null,
      visibility = null,
      notificationsAllowed = false;

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

  function initVisibility() {
    if (typeof document.hidden !== "undefined") {
      visibility = {
        hidden: "hidden",
        visibilityChange: "visibilitychange",
        state: "visibilityState"
      };
    } else if (typeof document.mozHidden !== "undefined") {
      visibility = {
        hidden: "mozHidden",
        visibilityChange: "mozvisibilitychange",
        state: "mozVisibilityState"
      };
    } else if (typeof document.msHidden !== "undefined") {
      visibility = {
        hidden: "msHidden",
        visibilityChange: "msvisibilitychange",
        state: "msVisibilityState"
      };
    } else if (typeof document.webkitHidden !== "undefined") {
      visibility = {
        hidden: "webkitHidden",
        visibilityChange: "webkitvisibilitychange",
        state: "webkitVisibilityState"
      };
    } else {
      visibility = {};
    }
  }

  function isBackground() {
    if (!visibility) initVisibility();
    return visibility.hidden && document[visibility.hidden];
  }

  function initNotifications() {
    if (window.notifications) notifications = window.notifications;
    else if (window.webkitNotifications) notifications = window.webkitNotifications;
    else notifications = null;
    if (notifications) {
      var checkbox = $("#allow-notifications");
      notificationsAllowed = (notifications.checkPermission() === 0);
      checkbox.change(function(){
        notificationsAllowed = checkbox.attr("checked");
        if (notificationsAllowed) notifications.requestPermission();
      });
    }
    else {
      $("#notifications-form").hide();
    }
  }

  function notifiy(icon, title, content) {
    if (isBackground() && notificationsAllowed && notifications && notifications.checkPermission() === 0) {
      var notif = notifications.createNotification(icon, title, content);
      notif.show();
    }
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
    var text = (msg.type === 'info') ? (msg.author.name + ' ' + msg.text) : msg.text;
    var text = markdown.toHTML(text.replace(/^#/, ' #'));
    var fragment = '<article class="'+ msg.type +'" data-id="' + msg.id + '">' +
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
    if (msg.type === "message") notifiy('/assets/images/notif.png', msg.author.name, msg.text);
  }

  function send(msg) {
    socket.send(JSON.stringify(msg));
  }

  function initSocket(roomName, _socket) {
    socket = _socket;
    socket.onmessage = function(event) {
      var msg = JSON.parse(event.data);
      displayMessage(msg);
    };
    socket.onopen = function(event) {};
    socket.onclose = function(event) {};
    socket.onerror = function(event) {};
  }

  function init(){
    initNotifications();
  }

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

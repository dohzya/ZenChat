window.ZenChat = (function(){
  "use strict";

  var ZC = {
    input: null,
    source: null
  };

  ZC.initAjaxPost = function() {
    $("#input-form").submit(function(e){
      e.preventDefault();
      var data =
      $.ajax({
        type: "POST",
        url: "/message",
        data: {
          "text": ZC.input.val()
        },
        success: function(data, textStatus, jqXHR){
          ZC.input.val("");
        },
        dataType: "json"
      });
    });
  };

  ZC.initServerSentEvents = function() {
    ZC.source = new EventSource('/listen');
    ZC.source.addEventListener('message', function(e) {
      console.log(data);
      var data = JSON.parse(e.data);
      console.log(data);
      var fragment = '<article class="message" data-id="' + data.id + '">' +
         '<header>' +
           '<div class="nickname">'+data.author.name+'</div>' +
           '<div class="avatar"><img src="'+data.author.avatar+'" class="img-rounded" alt="'+data.author.name+'" /></div>' +
         '</header>' +
         '<section>' +
           '<p>'+data.text+'</p>' +
         '</section>' +
         '<footer>' +
           '<div class="time">'+data.date+'</div>' +
         '</footer>' +
       '</article>';
       console.log(fragment);
      $("#messages").append(fragment);
    }, false);

    ZC.source.addEventListener('open', function(e) {
      // Connection was opened.
    }, false);

    ZC.source.addEventListener('error', function(e) {
      if (e.readyState == EventSource.CLOSED) {
        // Connection was closed.
      }
    }, false);
  };

  ZC.init = function(){
    ZC.input = $("#input");
    if (!!window.EventSource) {
      ZC.initServerSentEvents();
      ZC.initAjaxPost();
    }
  };
  return ZC;
})();

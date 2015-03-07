var playerList, chatOutput, readyCheckModal;
var readyCheckEventId;
var socket;

$(function() {
  playerList = $(".chat .list-group");
  chatOutput = $(".chat .output");
  readyCheckModal = $(".ready-check.modal");
  $(".chat input").keydown(function(e) {
    if (e.keyCode == 13) {
      var text = $(this).val().trim();
      $(this).val("");
      if (text.length == 0) {
        return;
      }
      socket.send({
        command : "chat",
        text : text
      });
    }
  });

  $(".ready-check.modal .btn-success").click(function() {
    socket.send({
      command : "ready",
      event : readyCheckEventId
    });
    readyCheckModal.modal("hide");
  });
  $(".ready-check.modal .btn-danger").click(function() {
    socket.send({
      command : "not-ready",
      event : readyCheckEventId
    });
    readyCheckModal.modal("hide");
  });

  initChat();
});

function initChat() {
  socket = connect("$WEBSOCKET_IP", $WEBSOCKET_PORT).open(function() {
    console.log("connected!");

    socket.send({
      command : "login",
      token : $.cookie("token"),
    });
  }).message(handleMessage).close(function() {
    addSystemMessage("Disconnected from chat.");
  });
}

function handleMessage(e) {
  var dataArray = JSON.parse(e.data);
  for (var i = 0; i < dataArray.length; i++) {
    var data = dataArray[i];
    var command = data.command;
    if (command == "join") {
      var row = $("<a href='/players/" + data.battleId + "' class='player list-group-item' data-id='" + data.id + "'>")
          .text(data.name);
      playerList.append(row);
    } else if (command == "leave") {
      $("a[data-id='" + data.id + "']").remove();
    } else if (command == "chat") {
      var row = $("<div>").append($("<b>").text(data.name + ": ")).append($("<span>").text(data.text));
      chatOutput.append(row);
      chatOutput.scrollTop(chatOutput[0].scrollHeight);
    } else if (command == "ready-check") {
      $("a.player").removeClass("ready").removeClass("not-ready");
      addSystemMessage(data.sender + " has started a ready-check!");
      readyCheckEventId = data.event;
      new buzz.sound("//wowimg.zamimg.com/wowsounds/567478").play();
      readyCheckModal.modal("show");
    } else if (command == "sys-msg") {
      addSystemMessage(data.text);
    } else if (command == "ready") {
      getPlayer(data.player).addClass("ready").removeClass("not-ready");
    } else if (command == "not-ready") {
      getPlayer(data.player).addClass("not-ready").removeClass("ready");
    } else if (command == "next-round") {
      new buzz.sound("//wowimg.zamimg.com/wowsounds/567399").play();
    } else if (command == "sound") {
      new buzz.sound(data.url).play();
    } else {
      console.log("Unknown command: " + command);
    }
  }
}

function getPlayer(player) {
  return $("a.player").filter(function() {
    return $(this).text() == player;
  });
}

function addSystemMessage(msg) {
  var row = $("<div class='sys-msg'>").append($("<span>").html(msg));
  chatOutput.append(row);
  chatOutput.scrollTop(chatOutput[0].scrollHeight);
}

function connect(ip, port) {
  if (!window.WebSocket) {
    console.log("websockets not supported!");
    return;
  }

  var socket = new WebSocket("ws://" + ip + ":" + port + "/");

  return {
    send : function(data) {
      socket.send(JSON.stringify(data));
      return this;
    },
    open : function(e) {
      socket.onopen = e;
      return this;
    },
    close : function(e) {
      socket.onclose = e;
      return this;
    },
    message : function(e) {
      socket.onmessage = e;
      return this;
    }
  };
}

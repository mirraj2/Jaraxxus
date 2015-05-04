var playerList, chatOutput, readyCheckModal, countdown;
var readyCheckEventId;
var socket;
var numPlayers;
var isAdmin = false, ready = false;

$(function() {
  playerList = $(".chat .list-group");
  chatOutput = $(".chat .output");
  readyCheckModal = $(".ready-check.modal");
  countdown = $(".status .countdown");
  numPlayers = 0;

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

  function updateLayout() {
    chatOutput.height($(window).height() - 140);
    playerList.height($(window).height() - 140);
  }

  $(window).on("resize", updateLayout);
  updateLayout();

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
      $(".player-counter").text(++numPlayers);
    } else if (command == "leave") {
      $("a[data-id='" + data.id + "']").remove();
      $(".player-counter").text(--numPlayers);
    } else if (command == "chat") {
      var from = $("<b>").toggleClass("admin", data.admin).text(data.name + ": ");
      if (isAdmin) {
        from = $("<a href='/chatprofile/" + data.id + "'>").append(from);
      }
      var msg = $("<span>").text(data.text);
      var row = $("<div>").append(from).append(msg);
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
    } else if (command == "next-round" || command == "match-info") {
      if (data.match) {
        $(".status .countdown").addClass("hidden");
        console.log(window.location.pathname);
        if (window.location.pathname != "/match/" + data.match) {
          $(".status .match h3").text(
              "Round " + data.round + (command == "next-round" ? " has begun!" : " is in progress."));
          $(".status .match a").attr("href", "/match/" + data.match);
          $(".status .match").removeClass("hidden");
        }
      }
      if (command == "next-round") {
        new buzz.sound("//wowimg.zamimg.com/wowsounds/567399").play();
      }
    } else if (command == "sound") {
      new buzz.sound(data.url).play();
    } else if (command == "admin") {
      isAdmin = true;
    } else if (command == "upcoming_event") {
      if(countdown.hasClass("hidden")){
        countdown.find("a").attr("href", "/events/" + data.id);
        countdown.find("h3 a").text(data.name);
        countdownDate = new Date(data.date);
        refreshCountdown();
        setInterval(refreshCountdown, 1000);
        ready = data.ready;
        syncReady();
        countdown.removeClass("hidden");
      }
    } else {
      console.log("Unknown command: " + command);
    }
  }
}

function syncReady() {
  countdown.find(".ready").toggleClass("hidden", !ready);
  countdown.find(".not-ready").toggleClass("hidden", ready);
}

function refreshCountdown() {
  var seconds = Math.round((countdownDate - new Date()) / 1000);

  var text = "";
  if (seconds > 0) {
    var hours = Math.floor(seconds / 60 / 60);
    seconds -= hours * 60 * 60;
    var minutes = Math.floor(seconds / 60);
    seconds -= minutes * 60;

    if (hours == 0 && minutes <= 30 && !ready) {
      ready = true;
      syncReady();
      socket.send({
        command : "ready"
      });
    }

    if (hours > 0) {
      text += hours + " hour";
      if (hours > 1) {
        text += "s";
      }
      text += " ";
    }
    if (minutes > 0 || hours > 0) {
      text += minutes + " minutes ";
    }
    if (hours == 0) {
      text += seconds + " seconds";
    }
  } else {
    text = "0 seconds";
  }

  $(".countdown .starts-in").text(text);
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

$("#status").change(function() {
  var val = $(this).val();
  $.post("/events/" + eventId + "/setStatus", {
    "status" : val
  }).fail(function() {
    alert("There was a problem processing this request.");
  });
});

$("table .match").click(function() {
  var id = $(this).data("id");
  window.location = "/match/" + id;
});

$("button.ready-check").click(function() {
  socket.send({
    command : "issue-ready-check",
    event : eventId
  });
});

$("button.announce").click(function() {
  var text = prompt("Enter announcement:");
  if (!text) {
    return;
  }
  socket.send({
    command : "announce",
    text : text
  });
});

$("button.kick").click(function() {
  var playerId = $(this).data("id");
  $(this).parent().remove();
  $.post("/events/" + eventId + "/kick/" + playerId).fail(function() {
    alert("There was a problem kicking this player.");
  });
});
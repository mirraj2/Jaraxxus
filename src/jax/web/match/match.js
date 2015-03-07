$(".game-result button").click(function() {
  $(this).parent().children("button").removeClass("btn-success").addClass("btn-default");
  $(this).addClass("btn-success");

  var index = $(this).parent(".game-result").data("index");

  var player = $(this).data("player");
  $.post("/match/" + matchId + "/result/" + index, {
    winner : player
  }).fail(function(e) {
    alert(e.responseText);
  });
});

$(".game-result").each(function() {
  var winnerA = $(this).data("winnera");
  var winnerB = $(this).data("winnerb");
  $(this).children("button").each(function() {
    var player = $(this).data("player");
    if (player == winnerA || player == winnerB) {
      $(this).removeClass("btn-default").addClass("btn-success");
    }
  });
});

$(".battle-tag").click(function() {
  selectText($(this).get(0));
}).mouseover(function() {
  selectText($(this).get(0));
});

$('[data-toggle="tooltip"]').tooltip();

function selectText(text) {
  var doc = document, range, selection;
  if (doc.body.createTextRange) {
    range = document.body.createTextRange();
    range.moveToElementText(text);
    range.select();
  } else if (window.getSelection) {
    selection = window.getSelection();
    range = document.createRange();
    range.selectNodeContents(text);
    selection.removeAllRanges();
    selection.addRange(range);
  }
}
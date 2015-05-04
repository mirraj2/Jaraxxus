function getTimeZone(date){
  if(!date){
    date = new Date();
  }
  return /\((.*)\)/.exec(date.toString())[1];
}

$(function(){
  $(".date").each(function(){
    var date = new Date($(this).text());
    var formatted = $.format.date(date, "MMMM d @ h:mm a ") + getTimeZone(date);
    $(this).text(formatted);
  });
  
  $('[data-toggle="tooltip"]').tooltip()
});

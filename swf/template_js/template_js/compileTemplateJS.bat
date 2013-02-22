..\swfmill\swfmill simple ..\base\player.xml player_flv_js.swf
..\mtasc\mtasc -version 7 -keep -strict -v -main -cp ..\classes -cp ..\mtasc\std8\ -swf player_flv_js.swf TemplateJS.as
copy player_flv_js.swf ..\..\..\assets\
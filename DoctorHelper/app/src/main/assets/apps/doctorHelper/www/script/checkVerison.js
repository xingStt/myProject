function checkVersion(serverIp,b){
	var reqUrl = serverIp+"/deskNurse/desk/checkVersion.do"; 
	var appVersion;
	plus.runtime.getProperty(plus.runtime.appid, function(inf) {
		appVersion = inf.version;
		$.ajax({
	        type: 'POST',
	        url: reqUrl,
	        data: {'appVersion':appVersion,'type':'2'},
	        dataType: "json",
	        async:false,
	        success: function(data){
	        	if(data.update){
	        		var btnArray = ['否', '是'];
				    mui.confirm('确认下载升级包','有新版本可更新', btnArray, function(e) 
				    {
				        if (e.index == 1) 
				        {
				        	downloadApp(serverIp,data.versionCode);
				        }
				    })
				}else{
					mui.toast("没有版本可更新");
				}
	        }
	    });
	});
}

function downloadApp(serverIp,versionCode){
	$('#downloading').show();
	var url = serverIp+"/deskNurse/desk/downloadFile.do?versionCode="+versionCode;
	var dtask = plus.downloader.createDownload(url, {}, function(d, status) {
		if(status == 200) { // 下载成功
			$('#downloading').hide();
			var path = d.filename;
			installWgt(d.filename);
		} else { //下载失败
			alert("Download failed: " + status);
		}
	}).start();
}
// 更新应用资源
function installWgt(path){
    plus.runtime.install(path,{},function(){
        plus.nativeUI.closeWaiting();
        plus.nativeUI.alert("应用资源更新完成！",function(){
            plus.runtime.restart();
        });
    },function(e){
        plus.nativeUI.closeWaiting();
        plus.nativeUI.alert("安装wgt文件失败["+e.code+"]："+e.message);
    });
}
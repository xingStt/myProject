document.addEventListener("plusready", function() {
	var B = window.plus.bridge /*插件调用桥梁*/ ;
	var mediaRecorder = {
		"startRecorder": function(successCallback, errorCallback) {
			var success = typeof successCallback !== 'function' ? null : function(args) {
					successCallback(args);
				},
				fail = typeof errorCallback !== 'function' ? null : function(code) {
					errorCallback(code);
				},
				callbackID = B.callbackId(success, fail);
			return B.exec("MediaRecorder", "startRecorder", [callbackID]);
		}
	};
	var fullScreen = {
		"startPlay": function(webUrl,curtime, successCallback, errorCallback) {
			var success = typeof successCallback !== 'function' ? null : function(args) {
					successCallback(args);
				},
				fail = typeof errorCallback !== 'function' ? null : function(code) {
					errorCallback(code);
				},
				callbackID = B.callbackId(success, fail);
			return B.exec("FullScreen", "startPlay", [callbackID, webUrl,curtime]);
		}
	};
	var localCamera = {
        "startCamera": function(successCallback, errorCallback) {
            var success = typeof successCallback !== 'function' ? null : function(args) {
                    successCallback(args);
                },
                fail = typeof errorCallback !== 'function' ? null : function(code) {
                    errorCallback(code);
                },
                callbackID = B.callbackId(success, fail);
            return B.exec("LocalCamera", "startCamera", [callbackID]);
        }
    };

	window.plus.mediaRecorder = mediaRecorder;
	window.plus.fullScreen = fullScreen;
	window.plus.localCamera = localCamera;
}, true);
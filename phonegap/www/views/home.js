wathack.home = function () {
	var files = new DevExpress.data.DataSource({store:[], paginate: false});
	var GPS_POS = {lat: 0, lon: 0};
	var selected_file = null;
	var loaded_file = null;


	var uploading_file_type = "";
	var uploading_file = null;

	function get_time_difference(earlierDate,laterDate){
       var nTotalDiff = laterDate.getTime() - earlierDate.getTime();
       var oDiff = new Object();
 
       oDiff.days = Math.floor(nTotalDiff/1000/60/60/24);
       nTotalDiff -= oDiff.days*1000*60*60*24;
 
       oDiff.hours = Math.floor(nTotalDiff/1000/60/60);
       nTotalDiff -= oDiff.hours*1000*60*60;
 
       oDiff.minutes = Math.floor(nTotalDiff/1000/60);
       nTotalDiff -= oDiff.minutes*1000*60;
 
       oDiff.seconds = Math.floor(nTotalDiff/1000);
 
       return oDiff;
 
		}

	function getData(){
		$.ajax({ 
	        url:"http://caffeine.csclub.uwaterloo.ca:40080/list?latitude="+GPS_POS.lat+"&longitude="+GPS_POS.lon, 
	        dataType: "json",
	        async: false,
	        error: function(jqXHR, textStatus, errorThrown){
	            alert(errorThrown);
	        }
	        }).done( function (data) {
	        	console.log(data);
	        	for (var i = 0; i < data.length; i++) {
	        		var created = new Date(data[i].created);
	        		var today = new Date();
	        		var expire = new Date(data[i].expiration);
	        		var string = "";
	        		//how far
	        		var diff = get_time_difference(created, today);
	        		if(diff.days > 0)
	        			string += diff.days+" day(s) ago, ";
	        		else if(diff.hours > 0)
	        			string += diff.hours+" hour(s) ago, ";
	        		else 
	        			string += diff.minutes+" minutes ago, ";

	        		//how left
	        		diff = get_time_difference(today, expire);
	        		if(diff.days > 0)
	        			string += "expires in "+diff.days+" day(s)";
	        		else if(diff.hours > 0)
	        			string += "expires in "+diff.hours+" hour(s)";
	        		else 
	        			string += "expires in "+diff.minutes+" minute(s)";




					data[i].gone = string;        		

	        		if(data[i].content_type.indexOf("image") != -1)
	        			data[i].icon = "<img width='48' src='images/icons/file_types/image.png'/>";
	        		else if (data[i].content_type.indexOf("text") != -1)
	        			data[i].icon = "<img width='48' src='images/icons/file_types/message.png'/>";
	        		else
	        			data[i].icon = "<img width='48' src='images/icons/file_types/file.png'/>";
                    files.store().insert(data[i]);
                }
	        });
	}


	function onSuccess(position) {
		if(GPS_POS.lat != position.coords.latitude || GPS_POS.lon != position.coords.longitude){
			GPS_POS.lat = position.coords.latitude;
			GPS_POS.lon = position.coords.longitude;
			getData();
		}
		//console.log('Latitude: '+ position.coords.latitude+', Longitude: '+ position.coords.longitude);
	}
	function onSuccessUpload(position) {
		GPS_POS.lat = position.coords.latitude;
		GPS_POS.lon = position.coords.longitude;
		viewModel.loadPanelVisible(true);
        var params = {};
	    params.latitude = GPS_POS.lat;
	    params.longitude = GPS_POS.lon;
	    params.title = viewModel.slingTitle();
	    params.expiration = 43453453365;
	   	if(viewModel.slingPassword().length != 0) params.password = viewModel.slingPassword();



	    if(uploading_file_type == "image"){
	    	
        	var options = new FileUploadOptions();
	        options.fileKey="seed";
	        options.fileName=uploading_file.substr(uploading_file.lastIndexOf('/')+1);
	        options.mimeType="image/jpeg";

	        options.params = params;
	        var ft = new FileTransfer();
	        ft.upload(uploading_file, "http://caffeine.csclub.uwaterloo.ca:40080/plant", viewModel.uploaded, viewModel.didNot, options);

       	}
        else{

        }
		
		//console.log('Latitude: '+ position.coords.latitude+', Longitude: '+ position.coords.longitude);
	}
	function onErrorUpload(error) {
		alert("Yo GPS Error When Uploading - "+error.message);
		//console.log('Latitude: '+ position.coords.latitude+', Longitude: '+ position.coords.longitude);
	}
	function onError(error) {
		alert("Yo GPS Error - "+error.message);
	}

	var viewModel = {
		loadPanelVisible: ko.observable(false),

        uploaded:function(){
        	viewModel.hideUpload();
        	alert("The File was Slung!");
        	viewModel.loadPanelVisible(false);
        },
        didNot:function(){
        	alert("The File was not Slung.");
        },
        uploadPhoto: function(imageURI) {

            var options = new FileUploadOptions();
            options.fileKey="file";
            options.fileName=imageURI.substr(imageURI.lastIndexOf('/')+1);
            options.mimeType="image/jpeg";


            var params = {};
            params.value1 = "test";
            params.value2 = "param";

            var ft = new FileTransfer();
            ft.upload(imageURI, encodeURI("http://caffeine.csclub.uwaterloo.ca:40080/plant"), viewModel.uploaded, viewModel.didNot, options);
        },
        openCamera: function(){
        	uploading_file_type = "image";
        	navigator.camera.getPicture(viewModel.showUpload, function(message){
        		alert('Camera Roll Failed because: ' + message);
        	}, { quality: 50, destinationType: navigator.camera.DestinationType.FILE_URI, sourceType: navigator.camera.PictureSourceType.CAMERA });
        },
        cameraRoll:function(){
        	uploading_file_type = "image";
        	navigator.camera.getPicture(viewModel.showUpload, function(message){
        		alert('Camera Failed because: ' + message);
        	}, { quality: 50, destinationType: navigator.camera.DestinationType.FILE_URI, sourceType: navigator.camera.PictureSourceType.PHOTOLIBRARY });
        },
        getFileType:function(content_type){
        	var file_type = "other";
        		if(content_type.indexOf("image") != -1)
	        			file_type = "image";
	        	else if (content_type.indexOf("text") != -1)
	        			file_type = "text";

	        	return file_type;
        },
        listDataSource: files,
        fileVisible: ko.observable(false),
        addVisible: ko.observable(false),
        passwordVisible: ko.observable(false),
        uploadVisible: ko.observable(false),
        viewFile: function(file){
        	if(file.itemData.password == null){
        		var file_type = viewModel.getFileType(file.itemData.content_type);
        		viewModel.showFile(file.itemData.key, "SHAHMEER", file_type);
        	}
        	else{
        		viewModel.showPassword(file.itemData);
        	}
        },
        
        showFile: function(key, password, type){


        	var the_url = "http://caffeine.csclub.uwaterloo.ca:40080/get?key="+key+((password === "SHAHMEER")?"":"&password="+password);
        	console.log(the_url);
        	if(type=="image"){
        		loaded_file = "<img src="+the_url+" />";
        		viewModel.fileVisible(true);
        	}
        	else{
	        	$.ajax({ 
		        url:the_url, 
		        async: false,
		        error: function(jqXHR, textStatus, errorThrown){
		            alert("Error getting File Yo!!! "+errorThrown);
		        }
		        }).done( function (data) {
		        	loaded_file = data;
		        	viewModel.fileVisible(true);
		        });
		    }
	        

        },
        hideFile: function(){
        	$('#file_content').html("");
        	selectedFile= null;
        	viewModel.fileVisible(false);

        },
        showPassword: function(file){
        	selected_file = file;
        	viewModel.passwordVisible(true);

        },
        hidePass:function(){
        	viewModel.passwordVisible(false);
        },
        showUpload:function(data){
        	viewModel.hideAdd();
        	uploading_file = data;
        	viewModel.uploadVisible(true);

        },
        hideUpload:function(){
        	viewModel.uploadVisible(false);
        },
        refresh: function(){
        	navigator.geolocation.getCurrentPosition(onSuccess, onError);
        },
        showAdd: function(){
        	viewModel.addVisible(true);
        },
        hideAdd: function(){
        	viewModel.addVisible(false);
        },
        displayFile: function(){
        	if(loaded_file != null)
        		$('#file_content').html(loaded_file);
        },
        passwordInput: ko.observable(""),
        slingTitle: ko.observable(""),
        slingPassword: ko.observable(""),
        slingDie: ko.observable(-1),
        checkPass:function(){
        	//console.log(CryptoJS.SHA1("NSGbUvrdqwqHYFAQ"+viewModel.passwordInput()).toString());
        	//console.log(viewModel.passwordInput());
        	if(CryptoJS.SHA1("NSGbUvrdqwqHYFAQ"+viewModel.passwordInput()).toString() == selected_file.password){
        		viewModel.showFile(selected_file.key, viewModel.passwordInput(), viewModel.getFileType(selected_file.content_type));
        		viewModel.hidePass();
        		passwordInput("");
        	}

        },
        upload:function(){
        	navigator.geolocation.getCurrentPosition(onSuccessUpload, onErrorUpload);
        }
    };

	

	//navigator.geolocation.getCurrentPosition(onSuccess, onError);
	getData();

    return viewModel;
};
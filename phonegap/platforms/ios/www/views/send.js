wathack.home = function () {
	var files = new DevExpress.data.DataSource({store:[], pageSize:7, paginate: true});
	var GPS_POS = {lat: 0, lon: 0};

	function getData(){
		console.log("http://linux024.student.cs.uwaterloo.ca:40080/list?latitude="+GPS_POS.lat+"&longitude="+GPS_POS.lon+"&radius=10");
		$.ajax({ 
	        url:"http://linux024.student.cs.uwaterloo.ca:40080/list?latitude="+GPS_POS.lat+"&longitude="+GPS_POS.lon, 
	        dataType: "json",
	        async: false,
	        error: function(jqXHR, textStatus, errorThrown){
	            alert(errorThrown);
	        }
	        }).done( function (data) {
	        	console.log(data);
	        	for (var i = 0; i < data.length; i++) {
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
	function onError(error) {
		alert("Yo GPS Error - "+error.message);
	}

	var viewModel = {
        openCamera: function(){
        	navigator.camera.getPicture(function(imageData){
        		var image = document.getElementById('myImage');
		    	image.src = "data:image/jpeg;base64," + imageData;

        	}, function(message){
        		alert('Failed because: ' + message);
        	}, { quality: 50, destinationType: Camera.DestinationType.DATA_URL});
        },
        listDataSource: files,
        viewFile: function(file){
        	if(file.itemData.password == null){
        		viewModel.showFile(file.itemData.key);
        	}
        	else{
        		viewModel.showPassword(file.itemData.key);
        	}
        },
        showFile: function(key){
        	$("#file-content").fadeIn();


        },
        showPassword: function(key){
        	$("#password-content").fadeIn();

        },
        refresh: function(){
        	navigator.geolocation.getCurrentPosition(onSuccess, onError);
        },
        showAdd: function(){
        	
        }
    };

	

	navigator.geolocation.getCurrentPosition(onSuccess, onError);
	getData();

    return viewModel;
};
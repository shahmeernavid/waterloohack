wathack.home = function () {
	var getter = DevExpress.data.utils.compileGetter("distance");
	var files = new DevExpress.data.DataSource({store:[], load:getData, pageSize:7, paginate: true, filter: ["distance", "<", 10], sort:"created"});
	var GPS_POS = {lat: 0, lon: 0};
	var selectedFile = null;


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
        seeFile: function(){
        	wathack.app.navigate("file/" + selectedDelegate.id);
        }
    };

	function getData(){
		console.log(files.filter());
		return $.ajax({ 
	        url:"http://linux024.student.cs.uwaterloo.ca:40080/list?latitude="+GPS_POS.lat+"&longitude="+GPS_POS.lon, 
	        dataType: "json",
	        async: true,
	        error: function(jqXHR, textStatus, errorThrown){
	            alert(errorThrown);
	        }
	        }).done( function (data) {
	        });
	}

	function onSuccess(position) {
		if(GPS_POS.lat != position.coords.latitude || GPS_POS.lon != position.coords.longitude){
			
		}
		//console.log('Latitude: '+ position.coords.latitude+', Longitude: '+ position.coords.longitude);
		//navigator.geolocation.getCurrentPosition(onSuccess, onError);
	}
	function onError(error) {
		alert(error.message);
	}

	navigator.geolocation.getCurrentPosition(onSuccess, onError);


    return viewModel;
};
/**
 * 
 */

function getAllInterfaces(handler) {
	$.ajax({
		url:'rest/interface'
		}).done(function(interfaces) {
			handler(interfaces);
		});
}

function loadData(ifcId, params, handler) {
	$.ajax({
		url:'rest/data/' + ifcId
		}).success(function(data) {
			handler(data);
		}).error(function(jqXHR) {
			showAjaxError(jqXHR);
		});
}

function save(ifcId, data, handler) {
	$.ajax({
		type: 'post',
		url:'rest/data/' + ifcId,
		contentType: "application/json; charset=utf-8",
		data: JSON.stringify(data),
		dataType: "json"
		}).success(function(data) {
			handler(data);
		}).error(function(jqXHR) {
			showAjaxError(jqXHR);
		});
}

function execute(ifcId, actionId, data, handler) {
	$.ajax({
		type: 'post',
		url:'rest/data/' + ifcId + '/' + actionId,
		contentType: "application/json; charset=utf-8",
		data: JSON.stringify(data),
		dataType: "json"
		}).success(function(data) {
			handler(data);
		}).error(function(jqXHR) {
			showAjaxError(jqXHR);
		});
}

function showError(title, message) {
	$('#errorDlg .modal-title').html(title);
	$('#errorDlg .modal-body').html(message);
	$("#errorDlg").modal()
}

function showAjaxError(jqXHR) {
	showError(jqXHR.status + ' ' + jqXHR.statusText, jqXHR.responseText);
}
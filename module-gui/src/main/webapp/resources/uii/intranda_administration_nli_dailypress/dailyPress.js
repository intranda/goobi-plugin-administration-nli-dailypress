
$(document).ready(function() {
	
//	$('.hasDatepicker').datepicker({
//		format: "yyyy/mm/dd"
//	});
	
//	$("#cms_id_search_btn").on("click", function() {
//		$("#newspaper_metadata_waiting").show();
//	});
})

function executeNewspaperSearch(ajaxData) {
	console.log("execute newspaper search: " + ajaxData.status);
	if(ajaxData.status === "begin") {
		$("#newspaper_metadata_waiting").show();
		$("#newspaper_metadata").hide();
	} else if(ajaxData.status === "success") {
		$("#newspaper_metadata_waiting").hide();
		$("#newspaper_metadata").show();
		$("#missing_newspaper_metadata").show();
	}
	
}

<#include "page.ftl" />
<@page title="Sensoren">
	<form method='post' enctype='multipart/form-data'>
		<input type='file' name='uploaded_file'>
		<button>${action}</button>
	</form>
</@page>
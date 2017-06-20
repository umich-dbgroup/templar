<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="architecture.*" %>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>NaliR</title>

<link rel="stylesheet" type="text/css" href="_styles.css">
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script>
$(document).ready(function()
{
	$("#newQuery").click(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "newQuery " + $("#inputSentence").text() 
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
	$("#database").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "useDatabase " + $("#database").val() 
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
});

function feedback(data)
{
	var feedbacks = data.split("\n"); 
	var i = 0; 
	while(i < feedbacks.length)
	{
		var curFeedback = feedbacks[i].split(" "); 
		if(curFeedback[0] == "sentence")
		{
			document.getElementById("noMap").innerHTML = "";
			document.getElementById("map").innerHTML = "";
			document.getElementById("useless").innerHTML = "";
			document.getElementById("generalIntepretation").innerHTML = "";	
			document.getElementById("specificIntepretation").innerHTML = "";	
			document.getElementById("error").innerHTML = "";	
			document.getElementById("warning").innerHTML = "";	

			var sentence = ""; 
			while(i < feedbacks.length && curFeedback[0] == "sentence")
			{
				var word = curFeedback[2]; 
				for(var j = 3; j < curFeedback.length; j++)
				{
					word += " " + curFeedback[j]; 
				}
				sentence += "<U>" + word + "</U><sup><small>" + curFeedback[1] + "</small></sup> "; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			
			document.getElementById("checkInput").innerHTML = "<b>Your input is: </b>" + sentence;
		}
		else if(curFeedback[0] == "noMap")
		{
			var result = ""; 
			while(i < feedbacks.length && curFeedback[0] == "noMap")
			{
				result += curFeedback[1] + "<sup><small>" + curFeedback[2] + "</small></sup>, "; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			
			if(result.length > 0)
			{
				document.getElementById("noMap").innerHTML = result + " cannot be recognized by the system. ";
			}
			i++; 
		}
		else if(curFeedback[0] == "useless")
		{
			var result = ""; 
			while(i < feedbacks.length && curFeedback[0] == "useless")
			{
				var word = curFeedback[2]; 
				for(var j = 3; j < curFeedback.length; j++)
				{
					word += " " + curFeedback[j]; 
				}
				result += "<U>" + word + "</U><sup><small>" + curFeedback[1] + "</small></sup> "; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}

			if(result.length > 0)
			{
				document.getElementById("useless").innerHTML = "These words are considered useless: " + result;
			}
		}
		else if(curFeedback[0] == "map")
		{
			var results = "";
			while(i < feedbacks.length && curFeedback[0] == "map")
			{
				var result = ""; 
				choices = feedbacks[i].split("; "); 					
				
				if(choices.length > 3)
				{
					result += curFeedback[1] + "<sup><small>" + curFeedback[2] + "</small></sup>" + " <b>maps to</b> " 
					+ "<select class = 'map' id = \"" + curFeedback[2] + "\">"; 
				
					var selected = curFeedback[3].substring(0, curFeedback[3].length-1); 
					
					var stringName = ""; 
					for(var j = 1; j < choices.length-1; j++)
					{
						if(j == parseInt(selected)+1)
						{
							stringName = choices[j]; 
							result += "<option value = \"" + (j-1).toString() + "\" selected = \"selected\">" + choices[j] + "</option>"; 
						}
						else
						{
							result += "<option value = \"" + (j-1).toString() + "\">" + choices[j] + "</option>"; 
						}
					}
					result += "</select>"; 

					if(choices[choices.length-1].length > 0)
					{
						var specifies = choices[choices.length-1].split("# "); 
						if(specifies.length > 3)
						{
							result += " <b>specifically</b> " + "<select class = 'specify' id = \"" + curFeedback[2] + "\">"; 

							var selected = specifies[0]; 
							if(specifies.length > 3)
							{
								if(selected == -1)
								{
									result += "<option value = \"" + selected + "\" selected = \"selected\">" + "any " + stringName + " containing \"" + curFeedback[1] + "\"</option>"; 
								}
								else
								{
									result += "<option value = \"" + selected + "\">" + "any " + stringName + " containing " + curFeedback[1] + "</option>"; 
								}
							}
							for(var j = 1; j < specifies.length-1; j++)
							{
								if(j == parseInt(selected)+1)
								{
									result += "<option value = \"" + (j-1).toString() + "\" selected = \"selected\">" + specifies[j] + "</option>"; 
								}
								else
								{
									result += "<option value = \"" + (j-1).toString() + "\">" + specifies[j] + "</option>"; 
								}
							}
							result += "</select>"; 					
						}
					}
					result += "<br>"; 
					results += result; 
				}
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			if(results.length > 0)
			{
				document.getElementById("map").innerHTML = results;
			}
		}
		else if(curFeedback[0] == "general")
		{
			var result = "";
			var selected = curFeedback[1]; 
			i++; 
			var num = 0; 
			curFeedback = feedbacks[i].split(" "); 

			result += "Possible <b>approximate interpretations</b>: <br>"; 
			result += "<select id = \"setGeneralIntepretation\">"; 
			while(i < feedbacks.length && curFeedback[0] == "general")
			{
				var intepret = ""; 
				for(var j = 1; j < curFeedback.length; j++)
				{
					intepret += curFeedback[j] + " "; 
				}
				if(num == selected)
				{
					result += "<option value = \"" + num + "\" selected = \"selected\">" + intepret + "</option>"; 
				}
				else
				{
					result += "<option value = \"" + num + "\">" + intepret + "</option>"; 
				}
				i++;
				num++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</select>"; 

			if(result.length > 1)
			{
				document.getElementById("generalIntepretation").innerHTML = result;
			}
		}
		else if(curFeedback[0] == "specific")
		{
			var result = "";
			var selected = curFeedback[1]; 
			i++; 
			var num = 0; 
			curFeedback = feedbacks[i].split(" "); 

			result += "Possible <b>accurate interpretations</b>: <br>"; 
			result += "<select id = \"setSpecificIntepretation\">"; 
			while(i < feedbacks.length && curFeedback[0] == "specific")
			{
				var intepret = ""; 
				for(var j = 1; j < curFeedback.length; j++)
				{
					intepret += curFeedback[j] + " "; 
				}
				if(num == selected)
				{
					result += "<option value = \"" + num + "\" selected = \"selected\">" + intepret + "</option>"; 
				}
				else
				{
					result += "<option value = \"" + num + "\">" + intepret + "</option>"; 
				}
				i++;
				num++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</select>"; 

			if(result.length > 1)
			{
				document.getElementById("specificIntepretation").innerHTML = result;
			}
		}
		else if(curFeedback[0] == "result")
		{
			var result = "<b>Results: </b><br>"	+ "<table border = '1' style=\"margin-left:0.5em\">"; 
			var rowNum = -1; 
			while(i < feedbacks.length && curFeedback[0] == "result")
			{
				rowNum++; 
				var line = feedbacks[i].substring(7); 
				var row = line.split("###"); 
				result += "<tr>"; 
				for(var j = 0; j < row.length; j++)
				{
					result += "<td align = 'center' style = \"font-size:20px\">" + row[j] + "</td>"; 
				}
				result += "</tr>"; 
				i++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</table>"; 
			if(rowNum > 0)
			{
				document.getElementById("queryResults").innerHTML = result;
			}
			else
			{
				document.getElementById("queryResults").innerHTML = "<b>Result is Empty</b>";
			}
		}
		else if(curFeedback[0] == "feedback")
		{
			var results = ""; 
			while(i < feedbacks.length && curFeedback[0] == "feedback")
			{
				results += feedbacks[i]; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			if(results.length > 0)
			{
				document.getElementById("feedbacks").innerHTML = results;
			}
		}
		else
		{
			i++; 
		}
	}
	var errors = document.getElementById("noMap").innerHTML; 
	if(errors.length > 0)
	{
		document.getElementById("error").innerHTML = "<b>Errors: </b>";	
	}

	var warnings = document.getElementById("useless").innerHTML + document.getElementById("map").innerHTML + document.getElementById("entity").innerHTML
		+ document.getElementById("generalIntepretation").innerHTML + document.getElementById("specificIntepretation").innerHTML; 
	
	$(".map").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "map " + $(this).attr("id") + " " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
	
	$(".specify").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "specify " + $(this).attr("id") + " " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});

	$("#setGeneralIntepretation").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "generalIntepretation " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});

	$("#setSpecificIntepretation").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "specificIntepretation " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
}
</script>
<% 
SystemInterface system = new SystemInterface();  
session.setAttribute("system", system);  
%>
</head>

<body>
<h2><b><ins>Na</ins>tural <ins>L</ins>anguage <ins>I</ins>nterface over <ins>R</ins>elational Databases</b></h2>

<p>Use 
<select id = "database">
<option value = "DBLP">DBLP</option> 
<option value = "Yahoo!Movie">Yahoo!Movie</option> 
<option value = "MAS">Microsoft Academic Search</option> 
</select>
database.</p>

<p><b>Your query: </b><br>
<textarea id = "inputSentence" rows="1" cols="100">return me all the authors who have more papers than Bob on VLDB after 2005.</textarea>
<button id = "newQuery" type ="button">Submit</button></p>

<p id = "queryResults"></p>
<p id = "checkInput"></p>
<p id = "error"></p>
<p id = "noMap"></p>
<p id = "warning"></p>
<p id = "useless"></p>
<p id = "map"></p>
<p id = "entity"></p>
<p id = "generalIntepretation"></p>
<p id = "specificIntepretation"></p>
<p id = "feedbacks"></p>
</body>
</html>
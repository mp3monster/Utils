//license : http://creativecommons.org/licenses/by/4.0/
//Creative Commons License
//This work is licensed under a Creative Commons Attribution 4.0 International License
// CC BY

import groovy.json.*
import java.net.URLConnection
import javax.net.ssl.*
import groovy.transform.Field

DisplayAll = false // allows us to pretty print all the API calls if necessary
@Field final DisplayAllCLI = "-d"

Debug = false
// change to true for debug print statements

// if defaults are set then calling with the default values will attempted
def String uname = null
def String password = null
def String svr = null

@Field CompleteGatewayList = []
@Field CompleteGatewayIdList = []
@Field CompleteAPIList = []
@Field int totalAPICalls = 0
@Field CompleteAppsList = []
@Field CompleteActiveNodeList = []

@Field final DisplayHelp = "-h"

@Field final ALLGateways="ALL"

@Field final DefaultFileName ="report.csv"
@Field final FileNameCLIParam = "-f"
ReportFileName = DefaultFileName
// defines the filename to be used

// API call header property
@Field final Authorization = "Authorization"


@Field final DataTypeCLIParam="-t"
@Field final AppDataTypeShortLabel="apps"
@Field final AppDataTypeLabel="applications"
@Field final ApiDataTypeLabel="apis"
AppsList = 0
APIsList = 1
ListType = AppsList

@Field final ACTIVE = "ACTIVE"

// allow the category to be configurable against apiIds or appIds
DataGrouping = "apiIds"

// allow duration choicers of last7days, last30days, last365days
@Field final ReportDuration30DayLabel="last30days"
@Field final ReportDuration7DayLabel="last7days"
@Field final ReportDuration1DayLabel="last24hours"
@Field final ReportDurationYearLabel="last365days"
ReportDurationLabel = ReportDuration30DayLabel
@Field final Days365Duration=0
@Field final Days30Duration=1
@Field final Days7Duration=2
@Field final Days1Duration=3
ReportDurationId=Days30Duration
final DurationCLIParam = "-p" // for period

@Field final DAY="DAY"
@Field final MONTH="MONTH"
@Field final HOUR="HOUR"

LogicalGateway = "PROD"
LoglicalGatewayCLIParam = "-g"

@Field final SEP = "," // the delimiter/separator to use

// certificate by pass ====================
// http://codingandmore.blogspot.co.uk/2011/07/json-and-ssl-in-groovy-how-to-ignore.html

class OverideHostnameVerifier implements HostnameVerifier
{
	boolean verify(String hostname,
		SSLSession session)
		{return true}
}

	class TrustManager implements X509TrustManager
	{

		public java.security.cert.X509Certificate[] getAcceptedIssuers()
		{
			return null;
		}

		public void checkClientTrusted(
			java.security.cert.X509Certificate[] certs, String authType)
			{
			}

			public void checkServerTrusted(
				java.security.cert.X509Certificate[] certs, String authType) {	}
	}

TrustManager[] trustAllCerts = new TrustManager[1]
trustAllCerts[0] = new TrustManager()

			// ================================================================


void outputTotals (String timeQuantity)
{
	println ("============================================")
	println ("====               Stats                ====")
	println ("Total gateways      = " + CompleteGatewayList.size())
	println ("Total APIs         = " + CompleteAPIList.size())
	println ("Total API calls    = " + totalAPICalls + " over " + timeQuantity)
	println ("Total Apps         = " + CompleteAppsList.size())
	println ("Total Active nodes = " + CompleteActiveNodeList.size())
	println ("============================================")

}

void DisplayHelp()
{
	println ("================================================\nHelp:\n")
	println ("Mandatory to provide server-url username password e.g. https://1.2.3.4/ me myPassword\n")
	println ("Without these parameters the app will stop\n")
	println ("optional parameters:")
	println (DisplayAllCLI+" == displays all the activity information")
	println (DisplayHelp+" == this information")
	println ("")
	println (FileNameCLIParam+" == output filename, if not defined the default report.csv will be used")
	println (LoglicalGatewayCLIParam + " == the name of the gateway or environment e.g. Dev, Test, PreProduction, Production -- the value ALL will force the logic to evaluate ALL logical gateways")
	println (DataTypeCLIParam + " == whether the output should be based upon <APIs|APPs>" )
	println (DurationCLIParam + " == period that the data set will cover using the following numeric options:")
	println ("            0 : Last 365 Days  - returns a value per month")
	println ("            1 : Last 30  Days  - returns a value per day")
	println ("            2 : Last 7   Days - returns a value per day")
	println ("            3 : Last 1   Day  - returns a value per hour")
	println (" This will default to the Last 30 days")
	println (" The app if completing successfully will display some stats that will help understand licensing position")

	println ("================================================\n")
	System.exit(0)
}

	//===============
 	// handle CLI
	if (DisplayAll) {println ("at CLI with " + args.size() + " args\n" + args.toString())}
	if (args.size() > 0)
	{
		try
		{
			if (args.size() < 3 || (args[0] == DisplayHelp))
			{
				DisplayHelp()
			}
			svr = args [0]
			uname = args[1]
			password = args [2]

			if (DisplayAll) {println ("svr="+svr + "\nusername ="+uname+"\nPassword =" + password)}

			if (args.size() > 3)
			{
				int argIdx = 3
				while (argIdx < args.size())
				{
					switch (args[argIdx])
					{
						case DurationCLIParam:
						argIdx++
						if ((args.size() > argIdx) && (args[argIdx] != null))
						{
							try
							{
								int option = args[argIdx].toInteger()
								if ((option >= 0) && (option < 4))
								{
									ReportDurationId=option
								}
								else
								{
									println ("Option " + option + " Not available, continue with default")
								}
							}
							catch (Exception err)
							{
								println ("Error converting value provided - will use default")
							}
							argIdx++
						}
						break

						case DataTypeCLIParam:
							argIdx++
							if ((args.size() > argIdx) && (args[argIdx] != null))
							{
								args[argIdx] = args[argIdx].toLowerCase()
								if ((args[argIdx] == AppDataTypeShortLabel) ||
								(args[argIdx] ==AppDataTypeLabel))
								{
									ListType = AppsList
									argIdx++
								}
								else if (args[argIdx] == ApiDataTypeLabel)
								{
									ListType = APIsList
									argIdx++
								}
								else
								{
									println ("Ignoring value " + args[argIdx])
									argIdx++
								}
							}
							else
							{
								println ("Couldn't set gateway")
							}
						break

						case LoglicalGatewayCLIParam:
							argIdx++
							if ((args.size() > argIdx) && (args[argIdx] != null))
							{
								LogicalGateway = args[argIdx]
								println ("Will get data for " + LogicalGateway + " gateway(s)")
								argIdx++
							}
							else
							{
								println ("Couldn't set gateway")
							}
						break

						case FileNameCLIParam:
							argIdx++
							if ((args.size() > argIdx) && (args[argIdx] != null))
							{
								ReportFileName = args[argIdx]
								argIdx++
							}
							else
							{
								println ("Couldn't set filename")
							}
						break

						case DisplayHelp:
							DisplayHelp()
							argIdx++
						break

						case DisplayAllCLI:
							DisplayAll = true
							argIdx++
						break

						default:
							println ("Unknown parameter:" + args[argIdx] + " ignorting")
							argIdx++
					}
				}
			}
		}
		catch (Exception err)
		{
			if (Debug)
			{
				println (err.getMessage())
		  		err.printStackTrace()
		  }
		  DisplayHelp()
			System.exit(0)
		}
	}
	else
	{
		DisplayHelp()
	}

	// verify all the parameters
	try
	{
		assert (uname.size() > 0) : "No username"
		assert (password.size() > 0) : "No password"
		assert (svr.size() > 0) : "No server"
	}
	catch (Exception err)
	{
		println (err.getMessage()  + "\n")
		println ("Error 2")
		if (Debug) {	err.printStackTrace()}
		DisplayHelp()
		System.exit(0)
	}

///=================================================

// get the Ids of the apps or APIs as the same basic data structure is returned
// we can switch the query and then execute the same logic for the rest
HashMap getIds (int listType, String authString, String svr, ArrayList trackerList)
{
	String query = null
	HashMap result = new HashMap()

	switch (listType)
	{
		case AppsList:
		query = svr+"/apiplatform/management/v1/applications"
		break

		case APIsList:
		query = svr+"/apiplatform/management/v1/apis"
		break

		default:
		println ("Unexpected option")
	}

	def listURL = new URL(query).openConnection()
	listURL.setRequestProperty(Authorization, authString)
	JSONData = new JsonSlurper().parse(listURL.getInputStream())

	int count = JSONData.count
	for (int idx = 0; idx < count; idx++)
	{
		result.put (JSONData.items[idx].id.toInteger(), JSONData.items[idx].name)
		if (Debug){println ("Adding:(" + JSONData.items[idx].id + ")" + JSONData.items[idx].name)}

		if (!trackerList.contains (JSONData.items[idx].name))
		{
			trackerList.add(JSONData.items[idx].name)
		}
	}

	if (Debug){println ("Data retrieved for type " + listType + ":\n" +new JsonBuilder(JSONData).toPrettyString())}

return result
}

// this connects to the management server using the base url (svr) and credentials to retrieve the logical gateway information
// the gateways identified can be lomited by using the gatewayType (a string presentation of Prod etc) The gateway type is predicated on
// the use of a naming convention.
String  getGatewayIds (String svr, String authString, String gatewayType)
{
	String gwayTypeLower = gatewayType.toLowerCase()
	String query = svr + "/apiplatform/management/v1/gateways/"
	StringBuffer gatewayIds = new StringBuffer();

	if (DisplayAll)	{println ("Looking for "+gatewayType)}

	def queryURL = new URL(query).openConnection()
	queryURL.setRequestProperty(Authorization, authString)
	def JSONData = new JsonSlurper().parse(queryURL.getInputStream())
	// locate the gateway Ids based on environment

	if (Debug){println ("Gateways:\n" +new JsonBuilder(JSONData).toPrettyString())}

	for (int idx = 0; idx < JSONData.count; idx++)
	{
		String name = JSONData.items[idx].name.toLowerCase()
		if (name.startsWith(gwayTypeLower) || (gatewayType==ALLGateways))
		{
			if (gatewayIds.size() > 0)
			{
				gatewayIds.append(SEP)
			}
			gatewayIds.append(JSONData.items[idx].id)

			if (DisplayAll){println ("Gateway " + name + " (" +JSONData.items[idx].id+ ") identified")}

		}

		// irrespective of the gateway filtering note the gateway name
		if (!CompleteGatewayList.contains(name)) 
		{
			CompleteGatewayList.add (name)
			CompleteGatewayIdList.add(JSONData.items[idx].id)
		}	
	}

	if (DisplayAll){println ("Found:"+gatewayIds.toString())}
	return gatewayIds.toString()
}

// This retrieves information about the nodes for a specific gateway. The information is aggregated into a text structure,
// and the global list of gateway nodes is updated assuming the state filter is satisfied. Note a null state means ALL nodes
String getNodeDetails (String id, String authString, String svr, String filterState)
{
	String nodeInfo = ""
	String query = svr + "/apiplatform/management/v1/gateways/"+id+"/nodes"
	def queryURL = new URL(query).openConnection()
	queryURL.setRequestProperty(Authorization, authString)
	def JSONData = new JsonSlurper().parse(queryURL.getInputStream())

	// if I have data thern ...
	if ((JSONData != null) && (JSONData.items != null))
	{
		// for each node entry ....
		for (int idx = 0; idx < JSONData.items.size(); idx++)
		{
			// if I will accept any state then check and add to my list and node info string
			if (filterState == null)
			{
				// count all states
				nodeInfo += JSONData.items[idx].name + " (" + JSONData.items[idx].state + ") updated at " + JSONData.items[idx].contactedAt + "\n"

				// if not yet in my node list - then add it -- we assume nodes are uniquely named
				if (!CompleteActiveNodeList.contains(JSONData.items[idx].name))
				{
					CompleteActiveNodeList.add(JSONData.items[idx].name)
				}
			}
			else
			{
				// i must be  state senstivie - do I have a match ?
				if (JSONData.items[idx].state == filterState)
				{
					nodeInfo += JSONData.items[idx].name + " (" + JSONData.items[idx].state + ") updated at " + JSONData.items[idx].contactedAt + "\n"

					// if not yet in my node list - then add it -- we assume nodes are uniquely named
					if (!CompleteActiveNodeList.contains(JSONData.items[idx].name))
					{
						CompleteActiveNodeList.add(JSONData.items[idx].name)
					}					
				}
			}

		}
	
	}
		
	return nodeInfo
}

// This takes the gateway Ids list and iterates through interogating the gateway about node details. The state value is passed through as a filter on the 
// node states accepted - as defined in the Oracle documentation. If the value is null then ALL nodes are interrogated
// the information is sent to concole if DisplayAll is set
void getGatewayNodes (ArrayList gatewayIds, String authString, String svr, String state)
{
	String nodeInfo = ""
	for (int idx = 0; idx < gatewayIds.size(); idx++)
	{
		// get node info

		nodeInfo = getNodeDetails (CompleteGatewayIdList[idx], authString, svr, state)

		if (DisplayAll) 
		{
			if (nodeInfo != null)
			{
				println ("Nodes for Gateway :"+ CompleteGatewayList[idx] + "("+ CompleteGatewayIdList[idx]+")")
				println (nodeInfo)
			}
		}

	}	
}

// derives the header information from a query result
String createCSVHeaderRow (Object jsonObj, String label)
{
	StringBuffer sb = new StringBuffer()

 if (Debug){println ("row obj " + rowName + ":\n" +new JsonBuilder(jsonObj).toPrettyString())}

	sb.append (label)
	if (jsonObj != null)
	{

		for (int idx = 0; idx < jsonObj.count; idx++)
		{
			sb.append(SEP)
			sb.append (jsonObj.items[idx].start_ts)
		}
	}

return sb.toString()
}

// this does assume we haven't undeployed the API during the reporting period
String createCSVRow (Object jsonObj, String rowName, int maxValues)
{
	StringBuffer sb = new StringBuffer()

 	if (Debug){println ("row obj " + rowName + ":\n" +new JsonBuilder(jsonObj).toPrettyString())}

	sb.append (rowName)
	if (jsonObj != null)
	{
		int initial = 0
		int subtotalCalls = 0

		if (jsonObj.count > maxValues)
		{
			// if there are more data items in the resultset retrieved than expected -
			// just grab the latest values by moving the array index up
			initial = jsonObj.count - maxValues
		}
		else if (jsonObj.count < maxValues)
		{
			// pad the string buffer with 0 values
			for (int padIdx = 0; padIdx < (maxValues - jsonObj.count); padIdx++)
			{
				sb.append(",0")
			}
		}
		for (int idx = initial; idx < jsonObj.count; idx++)
		{
			sb.append(SEP)
			sb.append (jsonObj.items[idx].measure)

			// capture the total number of API calls
			if (jsonObj.items[idx].measure != null)
			{
				subtotalCalls = subtotalCalls + jsonObj.items[idx].measure.toInteger()
			}

		}

		if (DisplayAll){println ("Total call count for " + rowName + "="+ subtotalCalls)}
		totalAPICalls += subtotalCalls

	}
	return sb.toString()
}
//==================================================
// main

		File file = new File(ReportFileName)

		if (file.exists())
		{
			file.delete()
			if (DisplayAll){println ("deleted old version of " + ReportFileName)}
		}
		file.createNewFile()

		// setup password string
		final String authStringPlain = uname+":"+password
		final authString = "Basic " + (authStringPlain.getBytes().encodeBase64().toString())
		// configure HTTP connectivity inc ignoring certificate validation
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier((HostnameVerifier)new OverideHostnameVerifier());

		try
		{

			gatewayIds = getGatewayIds (svr, authString, LogicalGateway)

			String dataType = AppDataTypeLabel
			String timeQuantity= ReportDuration30DayLabel
			String timeUnit = DAY
			String idParam = "apiIds"
			ArrayList trackerList

			def groupIds = "100"

			Boolean HeaderRow = true

			switch (ListType)
			{
				case AppsList:
				dataType = AppDataTypeLabel
				trackerList = CompleteAppsList
				idParam = "appIds"
				break

				case APIsList:
				dataType = ApiDataTypeLabel
				trackerList = CompleteAPIList
				idParam = "apiIds"
				break

				default:
				println ("Unexpected option")
			}

			switch (ReportDurationId)
			{
				case Days365Duration:
					timeUnit = MONTH
					timeQuantity=ReportDurationYearLabel
				break

				case Days30Duration:
					timeUnit = DAY
					timeQuantity=ReportDuration30DayLabel
				break

				case Days7Duration:
					timeUnit = DAY
					timeQuantity=ReportDuration7DayLabel

				break

				case Days1Duration:
					timeUnit = HOUR
					timeQuantity=ReportDuration1DayLabel
				break

				default:
				println ("Unknown interval defined")
			}

			int colCount = 0
			dataSet = getIds (ListType, authString, svr, trackerList)

			if (DisplayAll){println ("Gateways:"+gatewayIds+"| grouping by:"+DataGrouping + "| for period of " + timeQuantity + " |  unit " + timeUnit)}

			// iterate over this call for each API or App
			dataSet.each {id, name ->
				// need to go look up the descriptions
				String query = null

				query= svr + "/apiplatform/analytics/v1/timeSeries/requests/all"+"?gatewayIds="+gatewayIds+"&groupBys"+DataGrouping + "&timeSetting="+timeQuantity+ "&timeGroupSize=1&timeUnit="+timeUnit+"&"+idParam+"="+id


				def queryURL = new URL(query).openConnection()
				queryURL.setRequestProperty(Authorization, authString)
				def JSONData = new JsonSlurper().parse(queryURL.getInputStream())

				if (HeaderRow)
				{
					if (JSONData.count > 0)
					{
						line = createCSVHeaderRow (JSONData, dataType)
						colCount = line.count(SEP)

						file.append (line+"\n")
						if (DisplayAll){println ("Header created:"+line)}
						HeaderRow = false
					}
				}

				row = createCSVRow (JSONData, name, colCount)

				if (DisplayAll){println ("CSV line:" + row)}
				file.append (row + "\n")
			
			} // end of each

			switch (ListType)
			{
				case AppsList:
					// if the lookup is by App, get the APIs
					getIds (APIsList, authString, svr, CompleteAPIList)
				break

				case APIsList:
					// if the lookup is by api, get the apps
					getIds (AppsList, authString, svr, CompleteAppsList)
				break

				default:
				println ("Unexpected option")
			}

			getGatewayNodes (CompleteGatewayIdList, authString, svr, ACTIVE)
			outputTotals (timeQuantity)
		}
		catch (Exception err)
		{
			err.printStackTrace()
		}

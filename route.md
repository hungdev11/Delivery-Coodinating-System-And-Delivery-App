req

```json
{
	"startPoint": {
		"lat": 10.845792595570572,
		"lon": 106.77905428431495,
		"parcelId": "PARCEL-VQCQPN"
	},
	"priorityGroups": [
		{
			"priority": 1,
			"waypoints": [
				{
					"lat": 10.845842441229692,
					"lon": 106.78426753590685,
					"parcelId": "PARCEL-5Z5G6T"
				},
				{
					"lat": 10.846403056388937,
					"lon": 106.7828587234792,
					"parcelId": "PARCEL-2G4XDC"
				},
				{
					"lat": 10.845199200506443,
					"lon": 106.7799213853732,
					"parcelId": "PARCEL-R0JTIJ"
				}
			]
		},
		{
			"priority": 2,
			"waypoints": [
				{
					"lat": 10.846230216809758,
					"lon": 106.7840749916113,
					"parcelId": "PARCEL-6V0M80"
				}
			]
		}
	],
	"steps": true,
	"annotations": true,
	"mode": "flexible_priority_with_delta",
	"strategy": "strict_urgent"
}
```

response

```json
{
	"result": {
		"code": "Ok",
		"route": {
			"distance": 2032.3,
			"duration": 611.8,
			"geometry": "{\"type\":\"LineString\",\"coordinates\":[[106.779078,10.845834],[106.779202,10.845764],[106.779246,10.845739],[106.779402,10.845651],[106.779486,10.845604],[106.779785,10.845432],[106.779785,10.845432],[106.779706,10.845285],[106.77951,10.844978],[106.779315,10.844648],[106.779315,10.844648],[106.779513,10.84453],[106.779893,10.845179],[106.779911,10.845205],[106.779911,10.845205],[106.779911,10.845205],[106.779911,10.845205],[106.77999,10.845318],[106.77999,10.845318],[106.780061,10.84528],[106.780179,10.845211],[106.780884,10.844697],[106.78106,10.844575],[106.781169,10.844523],[106.781241,10.844484],[106.781333,10.844445],[106.781432,10.844402],[106.781432,10.844402],[106.781486,10.844473],[106.781719,10.844757],[106.781812,10.844869],[106.782217,10.845311],[106.782309,10.845411],[106.782555,10.845676],[106.782555,10.845676],[106.782587,10.845782],[106.782526,10.845894],[106.782471,10.845981],[106.782413,10.846081],[106.782404,10.846139],[106.782414,10.846187],[106.782434,10.846226],[106.782434,10.846226],[106.782523,10.846287],[106.782796,10.84643],[106.782862,10.846419],[106.782862,10.846419],[106.782862,10.846419],[106.782862,10.846419],[106.783064,10.846387],[106.783431,10.846435],[106.783699,10.846562],[106.783768,10.846522],[106.783904,10.84638],[106.78404,10.846303],[106.7842,10.846013],[106.784257,10.845892],[106.784275,10.845845],[106.784275,10.845845],[106.784275,10.845845],[106.784275,10.845845],[106.784455,10.845369],[106.784559,10.845012],[106.784772,10.844282],[106.784772,10.844282],[106.784546,10.844244],[106.784463,10.844231],[106.784138,10.844177],[106.783336,10.844045],[106.783116,10.844009],[106.782982,10.843987],[106.78276,10.843969],[106.782678,10.843975],[106.782632,10.843978],[106.782507,10.843993],[106.782325,10.844055],[106.781964,10.8442],[106.781668,10.844313],[106.781598,10.84434],[106.781432,10.844402],[106.781432,10.844402],[106.781486,10.844473],[106.781719,10.844757],[106.781812,10.844869],[106.782217,10.845311],[106.782309,10.845411],[106.782555,10.845676],[106.782555,10.845676],[106.782587,10.845782],[106.782526,10.845894],[106.782471,10.845981],[106.782413,10.846081],[106.782404,10.846139],[106.782414,10.846187],[106.782434,10.846226],[106.782434,10.846226],[106.782523,10.846287],[106.782796,10.84643],[106.783064,10.846387],[106.783431,10.846435],[106.783699,10.846562],[106.783768,10.846522],[106.783904,10.84638],[106.78404,10.846303],[106.784079,10.846232],[106.784079,10.846232],[106.784079,10.846232]]}",
			"legs": [
				{
					"distance": 302.5,
					"duration": 106.2,
					"steps": [
						{
							"distance": 89.2,
							"duration": 33.5,
							"instruction": "Continue on Lê Văn Việt",
							"name": "Unknown road",
							"maneuver": {
								"type": "depart",
								"modifier": "right",
								"location": [106.779078, 10.845834]
							},
							"geometry": {
								"coordinates": [
									[106.779078, 10.845834],
									[106.779202, 10.845764],
									[106.779246, 10.845739],
									[106.779402, 10.845651],
									[106.779486, 10.845604],
									[106.779785, 10.845432]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 101.2,
							"duration": 35.8,
							"instruction": "Turn right onto Đường Hồ Thị Tư",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "right",
								"location": [106.779785, 10.845432]
							},
							"geometry": {
								"coordinates": [
									[106.779785, 10.845432],
									[106.779706, 10.845285],
									[106.77951, 10.844978],
									[106.779315, 10.844648]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 112.1,
							"duration": 36.9,
							"instruction": "Continue on Đường Hồ Thị Tư",
							"name": "Unknown road",
							"maneuver": {
								"type": "continue",
								"modifier": "uturn",
								"location": [106.779315, 10.844648]
							},
							"geometry": {
								"coordinates": [
									[106.779315, 10.844648],
									[106.779513, 10.84453],
									[106.779893, 10.845179],
									[106.779911, 10.845205]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 0,
							"duration": 0,
							"instruction": "Arrive at your destination",
							"name": "Unknown road",
							"maneuver": {
								"type": "arrive",
								"location": [106.779911, 10.845205]
							},
							"geometry": {
								"coordinates": [
									[106.779911, 10.845205],
									[106.779911, 10.845205]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						}
					],
					"parcelId": "PARCEL-R0JTIJ"
				},
				{
					"distance": 511.4,
					"duration": 162.9,
					"steps": [
						{
							"distance": 15.2,
							"duration": 8.9,
							"instruction": "Head straight on Đường Hồ Thị Tư",
							"name": "Unknown road",
							"maneuver": {
								"type": "depart",
								"location": [106.779911, 10.845205]
							},
							"geometry": {
								"coordinates": [
									[106.779911, 10.845205],
									[106.77999, 10.845318]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 188.3,
							"duration": 56.8,
							"instruction": "Turn right onto Lê Văn Việt",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "right",
								"location": [106.77999, 10.845318]
							},
							"geometry": {
								"coordinates": [
									[106.77999, 10.845318],
									[106.780061, 10.84528],
									[106.780179, 10.845211],
									[106.780884, 10.844697],
									[106.78106, 10.844575],
									[106.781169, 10.844523],
									[106.781241, 10.844484],
									[106.781333, 10.844445],
									[106.781432, 10.844402]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 187.5,
							"duration": 39.6,
							"instruction": "Turn left onto Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "left",
								"location": [106.781432, 10.844402]
							},
							"geometry": {
								"coordinates": [
									[106.781432, 10.844402],
									[106.781486, 10.844473],
									[106.781719, 10.844757],
									[106.781812, 10.844869],
									[106.782217, 10.845311],
									[106.782309, 10.845411],
									[106.782555, 10.845676]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 67.4,
							"duration": 42,
							"instruction": "Continue on Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "fork",
								"modifier": "slight left",
								"location": [106.782555, 10.845676]
							},
							"geometry": {
								"coordinates": [
									[106.782555, 10.845676],
									[106.782587, 10.845782],
									[106.782526, 10.845894],
									[106.782471, 10.845981],
									[106.782413, 10.846081],
									[106.782404, 10.846139],
									[106.782414, 10.846187],
									[106.782434, 10.846226]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 53,
							"duration": 15.6,
							"instruction": "Turn right onto Hẻm 12 Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "right",
								"location": [106.782434, 10.846226]
							},
							"geometry": {
								"coordinates": [
									[106.782434, 10.846226],
									[106.782523, 10.846287],
									[106.782796, 10.84643],
									[106.782862, 10.846419]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 0,
							"duration": 0,
							"instruction": "Arrive at your destination",
							"name": "Unknown road",
							"maneuver": {
								"type": "arrive",
								"location": [106.782862, 10.846419]
							},
							"geometry": {
								"coordinates": [
									[106.782862, 10.846419],
									[106.782862, 10.846419]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						}
					],
					"parcelId": "PARCEL-2G4XDC"
				},
				{
					"distance": 200,
					"duration": 57.3,
					"steps": [
						{
							"distance": 200,
							"duration": 57.3,
							"instruction": "Head straight on Hẻm 12 Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "depart",
								"location": [106.782862, 10.846419]
							},
							"geometry": {
								"coordinates": [
									[106.782862, 10.846419],
									[106.783064, 10.846387],
									[106.783431, 10.846435],
									[106.783699, 10.846562],
									[106.783768, 10.846522],
									[106.783904, 10.84638],
									[106.78404, 10.846303],
									[106.7842, 10.846013],
									[106.784257, 10.845892],
									[106.784275, 10.845845]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 0,
							"duration": 0,
							"instruction": "Arrive at your destination",
							"name": "Unknown road",
							"maneuver": {
								"type": "arrive",
								"location": [106.784275, 10.845845]
							},
							"geometry": {
								"coordinates": [
									[106.784275, 10.845845],
									[106.784275, 10.845845]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						}
					],
					"parcelId": "PARCEL-5Z5G6T"
				},
				{
					"distance": 1018.4,
					"duration": 285.4,
					"steps": [
						{
							"distance": 182.2,
							"duration": 47,
							"instruction": "Head straight on Đường số 265",
							"name": "Unknown road",
							"maneuver": {
								"type": "depart",
								"location": [106.784275, 10.845845]
							},
							"geometry": {
								"coordinates": [
									[106.784275, 10.845845],
									[106.784455, 10.845369],
									[106.784559, 10.845012],
									[106.784772, 10.844282]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 376.3,
							"duration": 97.6,
							"instruction": "Continue on Lê Văn Việt",
							"name": "Unknown road",
							"maneuver": {
								"type": "end of road",
								"modifier": "right",
								"location": [106.784772, 10.844282]
							},
							"geometry": {
								"coordinates": [
									[106.784772, 10.844282],
									[106.784546, 10.844244],
									[106.784463, 10.844231],
									[106.784138, 10.844177],
									[106.783336, 10.844045],
									[106.783116, 10.844009],
									[106.782982, 10.843987],
									[106.78276, 10.843969],
									[106.782678, 10.843975],
									[106.782632, 10.843978],
									[106.782507, 10.843993],
									[106.782325, 10.844055],
									[106.781964, 10.8442],
									[106.781668, 10.844313],
									[106.781598, 10.84434],
									[106.781432, 10.844402]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 187.5,
							"duration": 39.6,
							"instruction": "Turn right onto Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "right",
								"location": [106.781432, 10.844402]
							},
							"geometry": {
								"coordinates": [
									[106.781432, 10.844402],
									[106.781486, 10.844473],
									[106.781719, 10.844757],
									[106.781812, 10.844869],
									[106.782217, 10.845311],
									[106.782309, 10.845411],
									[106.782555, 10.845676]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 67.4,
							"duration": 42,
							"instruction": "Continue on Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "fork",
								"modifier": "slight left",
								"location": [106.782555, 10.845676]
							},
							"geometry": {
								"coordinates": [
									[106.782555, 10.845676],
									[106.782587, 10.845782],
									[106.782526, 10.845894],
									[106.782471, 10.845981],
									[106.782413, 10.846081],
									[106.782404, 10.846139],
									[106.782414, 10.846187],
									[106.782434, 10.846226]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 204.8,
							"duration": 59.2,
							"instruction": "Turn right onto Hẻm 12 Trương Văn Thành",
							"name": "Unknown road",
							"maneuver": {
								"type": "turn",
								"modifier": "right",
								"location": [106.782434, 10.846226]
							},
							"geometry": {
								"coordinates": [
									[106.782434, 10.846226],
									[106.782523, 10.846287],
									[106.782796, 10.84643],
									[106.783064, 10.846387],
									[106.783431, 10.846435],
									[106.783699, 10.846562],
									[106.783768, 10.846522],
									[106.783904, 10.84638],
									[106.78404, 10.846303],
									[106.784079, 10.846232]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						},
						{
							"distance": 0,
							"duration": 0,
							"instruction": "Arrive at your destination",
							"name": "Unknown road",
							"maneuver": {
								"type": "arrive",
								"location": [106.784079, 10.846232]
							},
							"geometry": {
								"coordinates": [
									[106.784079, 10.846232],
									[106.784079, 10.846232]
								],
								"type": "LineString"
							},
							"addresses": [],
							"trafficLevel": "NORMAL"
						}
					],
					"parcelId": "PARCEL-6V0M80"
				}
			],
			"trafficSummary": {
				"averageSpeed": 11.958613926119648,
				"congestionLevel": "SLOW",
				"estimatedDelay": 368
			}
		},
		"visitOrder": [
			{
				"index": 0,
				"priority": 1,
				"priorityLabel": "express",
				"waypoint": {
					"lat": 10.845842441229692,
					"lon": 106.78426753590685,
					"parcelId": "PARCEL-5Z5G6T"
				}
			},
			{
				"index": 1,
				"priority": 1,
				"priorityLabel": "express",
				"waypoint": {
					"lat": 10.846403056388937,
					"lon": 106.7828587234792,
					"parcelId": "PARCEL-2G4XDC"
				}
			},
			{
				"index": 2,
				"priority": 1,
				"priorityLabel": "express",
				"waypoint": {
					"lat": 10.845199200506443,
					"lon": 106.7799213853732,
					"parcelId": "PARCEL-R0JTIJ"
				}
			},
			{
				"index": 3,
				"priority": 2,
				"priorityLabel": "fast",
				"waypoint": {
					"lat": 10.846230216809758,
					"lon": 106.7840749916113,
					"parcelId": "PARCEL-6V0M80"
				}
			}
		],
		"summary": {
			"totalDistance": 2032.3,
			"totalDuration": 611.8,
			"totalWaypoints": 4,
			"priorityCounts": {
				"express": 3,
				"fast": 1
			}
		}
	}
}
```

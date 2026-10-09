/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 0.0, "KoPercent": 100.0};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.0, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "GET protegido sin token -> 401"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente fechas -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "POST cliente body vacío -> 400"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH alta cuenta inválida -> 400"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH nueva cuenta activa -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET cuenta sintética -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET usuarios filtrados -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH nueva cuenta inactiva -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente correo -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente cuenta -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH reactivar cliente sintético -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente RFC -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente nombre -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET otro cliente denegado -> 403"], "isController": false}, {"data": [0.0, 500, 1500, "GET saldo propio -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET código postal válido -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET cuentas inactivas -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET propio cliente -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET cliente id 3 -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "POST login cliente -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "POST login ejecutivo -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET catálogo productos -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "PUT usuario duplicado -> 409"], "isController": false}, {"data": [0.0, 500, 1500, "GET lista cuentas denegada -> 403"], "isController": false}, {"data": [0.0, 500, 1500, "GET CP cacheado, carga baja -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET clientes -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET saldo sintético -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET cuentas cliente 3 -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "POST crear cuenta -> 201"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente apellido paterno -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET código postal inexistente -> 404"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente CURP -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET cuenta propia -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH datos inmutables -> 400"], "isController": false}, {"data": [0.0, 500, 1500, "POST cuenta como cliente denegado -> 403"], "isController": false}, {"data": [0.0, 500, 1500, "GET filtro cliente apellido materno -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "PATCH desactivar cliente sintético y cuentas -> 200"], "isController": false}, {"data": [0.0, 500, 1500, "GET token tras baja denegado -> 401"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 43, 43, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 12.824336415150611, 15.830040262451535, 0.0], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["GET protegido sin token -> 401", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente fechas -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["POST cliente body vacío -> 400", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH alta cuenta inválida -> 400", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH nueva cuenta activa -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET cuenta sintética -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET usuarios filtrados -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH nueva cuenta inactiva -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente correo -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente cuenta -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH reactivar cliente sintético -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente RFC -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente nombre -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET otro cliente denegado -> 403", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET saldo propio -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET código postal válido -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET cuentas inactivas -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET propio cliente -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET cliente id 3 -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["POST login cliente -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["POST login ejecutivo -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET catálogo productos -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PUT usuario duplicado -> 409", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET lista cuentas denegada -> 403", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET CP cacheado, carga baja -> 200", 6, 6, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 2.9925187032418954, 3.6938902743142146, 0.0], "isController": false}, {"data": ["GET clientes -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET saldo sintético -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET cuentas cliente 3 -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["POST crear cuenta -> 201", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente apellido paterno -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET código postal inexistente -> 404", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente CURP -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET cuenta propia -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH datos inmutables -> 400", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["POST cuenta como cliente denegado -> 403", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET filtro cliente apellido materno -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["PATCH desactivar cliente sintético y cuentas -> 200", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}, {"data": ["GET token tras baja denegado -> 401", 1, 1, 100.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, Infinity, Infinity, NaN], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 32, 74.4186046511628, 74.4186046511628], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 401 but received Non HTTP response code: java.lang.ClassCastException", 2, 4.651162790697675, 4.651162790697675], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 403 but received Non HTTP response code: java.lang.ClassCastException", 3, 6.976744186046512, 6.976744186046512], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 409 but received Non HTTP response code: java.lang.ClassCastException", 1, 2.3255813953488373, 2.3255813953488373], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 404 but received Non HTTP response code: java.lang.ClassCastException", 1, 2.3255813953488373, 2.3255813953488373], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 400 but received Non HTTP response code: java.lang.ClassCastException", 3, 6.976744186046512, 6.976744186046512], "isController": false}, {"data": ["Non HTTP response code: java.lang.ClassCastException/Expected HTTP 201 but received Non HTTP response code: java.lang.ClassCastException", 1, 2.3255813953488373, 2.3255813953488373], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 43, 43, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 32, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 403 but received Non HTTP response code: java.lang.ClassCastException", 3, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 400 but received Non HTTP response code: java.lang.ClassCastException", 3, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 401 but received Non HTTP response code: java.lang.ClassCastException", 2, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 409 but received Non HTTP response code: java.lang.ClassCastException", 1], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["GET protegido sin token -> 401", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 401 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente fechas -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST cliente body vacío -> 400", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 400 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH alta cuenta inválida -> 400", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 400 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH nueva cuenta activa -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET cuenta sintética -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET usuarios filtrados -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH nueva cuenta inactiva -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente correo -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente cuenta -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH reactivar cliente sintético -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente RFC -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente nombre -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET otro cliente denegado -> 403", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 403 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET saldo propio -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET código postal válido -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET cuentas inactivas -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET propio cliente -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET cliente id 3 -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST login cliente -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST login ejecutivo -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET catálogo productos -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PUT usuario duplicado -> 409", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 409 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET lista cuentas denegada -> 403", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 403 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET CP cacheado, carga baja -> 200", 6, 6, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 6, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET clientes -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET saldo sintético -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET cuentas cliente 3 -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST crear cuenta -> 201", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 201 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente apellido paterno -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET código postal inexistente -> 404", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 404 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente CURP -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET cuenta propia -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH datos inmutables -> 400", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 400 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST cuenta como cliente denegado -> 403", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 403 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET filtro cliente apellido materno -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["PATCH desactivar cliente sintético y cuentas -> 200", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 200 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET token tras baja denegado -> 401", 1, 1, "Non HTTP response code: java.lang.ClassCastException/Expected HTTP 401 but received Non HTTP response code: java.lang.ClassCastException", 1, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});

define("LCD/LCD_Part_Where_Used/assets/scripts/main", [
  "vue",
  "LCD/LCDLIB/scripts/vuetify.v2.6.13.min",
  "DS/WAFData/WAFData",
  "DS/PlatformAPI/PlatformAPI",
  "DS/DataDragAndDrop/DataDragAndDrop",
  "i18n!LCD/LCD_Part_Where_Used/nls/Part_Where_Used_nls",
  "i18n!LCD/LCD_Part_Where_Used/nls/Part_Where_Used_webservicedetails",
  "css!LCD/LCDLIB/styles/vuetify.v2.6.13.min.css",
  "css!LCD/LCDLIB/styles/materialdesignicons.min.css",
  "css!LCD/LCD_Part_Where_Used/assets/styles/style.css",
], function (
  Vue,
  Vuetify,
  WAFData,
  PlatformAPI,
  DataDragAndDrop,
  Part_Where_Used_nls,
  Part_Where_Used_webservicedetails
) {
  Vue.use(Vuetify, {});
  var myWidget = {
    _3dashboardUrl: widget.widgetDomain,
    _3dspaceUrl: "",
    onRefresh: function () {
      myWidget.onLoad();
    },
    onLoad: function () {
      var apps = PlatformAPI.getAllApplicationConfigurations();
      for (var i = 0; i < apps.length; i++) {
        if (apps[i]["propertyKey"] === "app.urls.myapps") {
          var u = new URL(apps[i]["propertyValue"]);
          myWidget._3dspaceUrl = u.href;
          break;
        }
      }
      new Vue({
        el: "#app",
        vuetify: new Vuetify(),
        template: `
        <v-app id="droppableArea">
        <p class="refreshClass">Last refreshed on: {{ date }} {{ time }}</p>
        <v-container>
          <h2 class="title">
            Part - Where Used
          </h2>
          <v-card-title>
            <span class="h1 me-4 partLabel">Part:</span>
    
            <v-form @submit.prevent="sendRequest()" v-model="formValidity" class="form">
              <v-text-field
                v-model="search"
                :rules="searchRules"
                label="Title"
                single-line
                outlined
                class="rounded-lg"
              ></v-text-field>

              <v-btn
                icons
                depressed
                rounded
                :disabled="!formValidity"
                color="white"
                type="submit"
                class="searchBtn"
              >
                <v-icon color="black">
                  mdi-magnify
                </v-icon>
              </v-btn>
            </v-form>
          </v-card-title>
    
          <v-data-table
            :headers="header"
            :items="filteredItems"
            :items-per-page="-1"
            fixed-header
            class="elevation-1 rounded-lg"
            :loading="loadingStatus"
          >
            <template v-slot:top>
              <v-btn
                color="#005685"
                class="ma-2 exportBtn"
                depressed
                :disabled="!result.length"
                @click="exportTableToCsvMethod(header, result)"
              >
                EXPORT
                <v-icon color="white" class="ps-2">
                  mdi-file-export
                </v-icon>
              </v-btn>
            </template>
    
            <template v-slot:header.level="{ header }">
              {{ header.text }}
              <v-menu offset-y :close-on-content-click="false">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn icon v-bind="attrs" v-on="on">
                    <v-icon small :color="searchLevel ? 'black' : 'white'">
                      mdi-magnify-expand
                    </v-icon>
                  </v-btn>
                </template>
                <div style="background-color: white; width: 280px">
                  <v-text-field
                    v-model="searchLevel"
                    class="pa-4"
                    type="text"
                    label="Enter the search term"
                    :autofocus="true"
                  ></v-text-field>
                  <v-btn
                    @click="searchLevel = ''"
                    small
                    text
                    color="primary"
                    class="ml-2 mb-2"
                    >Clean</v-btn
                  >
                </div>
              </v-menu>
            </template>
    
            <template v-slot:header.topNodeName="{ header }">
              {{ header.text }}
              <v-menu offset-y :close-on-content-click="false">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn icon v-bind="attrs" v-on="on">
                    <v-icon small :color="searchTopNodeName ? 'black' : 'white'">
                      mdi-magnify-expand
                    </v-icon>
                  </v-btn>
                </template>
                <div style="background-color: white; width: 280px">
                  <v-text-field
                    v-model="searchTopNodeName"
                    class="pa-4"
                    type="text"
                    label="Enter the search term"
                    :autofocus="true"
                  ></v-text-field>
                  <v-btn
                    @click="searchTopNodeName = ''"
                    small
                    text
                    color="primary"
                    class="ml-2 mb-2"
                    >Clean</v-btn
                  >
                </div>
              </v-menu>
            </template>
    
            <template v-slot:header.topNodeTitle="{ header }">
              {{ header.text }}
              <v-menu offset-y :close-on-content-click="false">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn icon v-bind="attrs" v-on="on">
                    <v-icon small :color="searchTopNodeTitle ? 'black' : 'white'">
                      mdi-magnify-expand
                    </v-icon>
                  </v-btn>
                </template>
                <div style="background-color: white; width: 280px">
                  <v-text-field
                    v-model="searchTopNodeTitle"
                    class="pa-4"
                    type="text"
                    label="Enter the search term"
                    :autofocus="true"
                  ></v-text-field>
                  <v-btn
                    @click="searchTopNodeTitle = ''"
                    small
                    text
                    color="primary"
                    class="ml-2 mb-2"
                    >Clean</v-btn
                  >
                </div>
              </v-menu>
            </template>
    
            <template v-slot:header.nodeMapArr="{ header }">
              {{ header.text }}
              <v-menu offset-y :close-on-content-click="false">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn icon v-bind="attrs" v-on="on">
                    <v-icon small :color="searchNavigation ? 'black' : 'white'">
                      mdi-magnify-expand
                    </v-icon>
                  </v-btn>
                </template>
                <div style="background-color: white; width: 280px">
                  <v-text-field
                    v-model="searchNavigation"
                    class="pa-4"
                    type="text"
                    label="Enter the search term"
                    :autofocus="true"
                  ></v-text-field>
                  <v-btn
                    @click="searchNavigation = ''"
                    small
                    text
                    color="primary"
                    class="ml-2 mb-2"
                    >Clean</v-btn
                  >
                </div>
              </v-menu>
            </template>
    
            <template v-slot:item.nodeMapArr="{ item }">
              <span v-if="item.nodeMapArr === 'Not Present'">
                Not Present
              </span>
              <span v-else v-for="(i, index) in item.nodeMapArr" :key="i.PartId">
                <a target="_blank" :href="i.PartId">
                  {{ i.PartTitle }}
                </a>
                <span v-if="item.nodeMapArr.length > index + 1">
                  {{ " → " }}
                </span>
              </span>
            </template>
          </v-data-table>
        </v-container>
      </v-app>
            `,
        data() {
          return {
            date: "",
            time: "",
            formValidity: false,
            search: "",
            searchRules: [
              (v) => !!v || "Part Title is required",
              (v) =>
                v.length >= 13 ||
                "Part title must be greater than and equal to 13",
              (v) =>
                /^[A-Z]\w{2}-[A-Z]\w{5}-\w{2}(\s\d{1,2}\.\d{1,2})?$/.test(v) ||
                "Part title must be valid",
            ],
            searchLevel: "",
            searchTopNodeName: "",
            searchTopNodeTitle: "",
            searchNavigation: "",
            header: Part_Where_Used_nls.headers,
            result: [],
            loadingStatus: false,
          };
        },
        mounted() {
          this.getCurrentTimestamp();
          this.handleDroppableArea();
        },
        computed: {
          filteredItems() {
            let conditions = [];

            if (this.searchLevel) {
              conditions.push(this.filterLevel);
            }
            if (this.searchTopNodeName) {
              conditions.push(this.filterTopNodeName);
            }
            if (this.searchTopNodeTitle) {
              conditions.push(this.filterTopNodeTitle);
            }
            if (this.searchNavigation) {
              conditions.push(this.filterNavigation);
            }
            if (conditions.length > 0) {
              return this.result.filter((item) => {
                return conditions.every((condition) => {
                  return condition(item);
                });
              });
            }
            return this.result;
          },
        },
        methods: {
          getCurrentTimestamp() {
            const date1 = new Date();
            const yyyy = date1.getFullYear();
            let mm = date1.getMonth() + 1; // Months start at 0!
            let dd = date1.getDate();
            if (dd < 10) dd = "0" + dd;
            if (mm < 10) mm = "0" + mm;
            const formattedToday = mm + "/" + dd + "/" + yyyy;
            // get the date as a string
            this.date = formattedToday;
            // get the time as a string
            this.time = date1.toLocaleTimeString();
          },
          handleDroppableArea() {
            let droppableArea = document.getElementById("droppableArea");
            DataDragAndDrop.clean(droppableArea);
            let counter = 0;
            DataDragAndDrop.droppable(droppableArea, {
              enter: () => {
                counter++;
                droppableArea.style.border = "4px dashed hwb(203deg 21% 23%)";
                droppableArea.style.opacity = "1";
              },
              drop: (json) => {
                counter = 0;
                droppableArea.style.border = "0";
                let data = JSON.parse(json);
                this.search =
                  data.data.items[0].displayName + " " + data.version;
                this.sendRequest();
              },
              leave: () => {
                counter--;
                if (counter == 0) {
                  droppableArea.style.border = "0";
                }
              },
            });
          },
          sendRequest() {
            this.result = [];
            this.loadingStatus = true;
            let reqNo = 0;
            let bomDataArr = widget.getValue("bomData");
            bomDataArr.forEach((bomData) => {
              let message = {
                partTitle: this.search.trim(),
                bomData: bomData,
              };
              let _this = this;
              let _myWidget = myWidget;
              reqNo += 1;
              WAFData.authenticatedRequest(
                _myWidget._3dspaceUrl +
                  Part_Where_Used_webservicedetails.getPartWhereUsedData,
                {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                  },
                  data: JSON.stringify(message),
                  accept: "application/json",
                  timeout: 200000,
                  onComplete: (res) => {
                    let formatedData = _this.formatUrl(JSON.parse(res));
                    _this.result = _this.result.concat(formatedData);
                    reqNo -= 1;
                    if (reqNo === 0) _this.loadingStatus = false;
                  },
                  onFailure: (err) => {
                    console.log(err);
                    reqNo -= 1;
                    if (reqNo === 0) _this.loadingStatus = false;
                  },
                }
              );
            });
          },
          formatUrl(data) {
            data.forEach((obj) => {
              if (obj.nodeMapArr.length > 0) {
                obj.nodeMapArr.forEach((nodeMap) => {
                  let asIsUrl =
                    myWidget._3dashboardUrl +
                    "/#app:ENOSCEN_AP/content:X3DContentId=";
                  let urlToEncode =
                    '{"data":{"items":[{"objectId":"' +
                    nodeMap.PartId +
                    '","objectType":"VPMReference","envId":"OnPremise","serviceId":"3DSpace"}]}}';
                  nodeMap.PartId = asIsUrl + encodeURIComponent(urlToEncode);
                });
              } else {
                obj.nodeMapArr = "Not Present";
              }
            });
            return data;
          },
          filterLevel(item) {
            return item.level.includes(this.searchLevel);
          },
          filterTopNodeName(item) {
            return item.topNodeName
              .toLowerCase()
              .includes(this.searchTopNodeName.toLowerCase());
          },
          filterTopNodeTitle(item) {
            return item.topNodeTitle
              .toLowerCase()
              .includes(this.searchTopNodeTitle.toLowerCase());
          },
          filterNavigation(item) {
            if (typeof item.nodeMapArr === "string") {
              return item.nodeMapArr
                .toLowerCase()
                .includes(this.searchNavigation.toLowerCase());
            } else {
              let filterData = item.nodeMapArr.reduce(
                (one, two) => (one += two.PartTitle + ","),
                ""
              );
              return filterData
                .toLowerCase()
                .includes(this.searchNavigation.toLowerCase());
            }
          },
          exportTableToCsvMethod(headers, rows) {
            const arrData = [];
            rows.map((row) => arrData.push(Object.assign({}, row)));

            let csvContent = "data:text/csv;charset=utf-8,";

            arrData.forEach((row) => {
              if (Array.isArray(row.nodeMapArr)) {
                let data = row.nodeMapArr
                  .map((nav) => nav.PartTitle)
                  .join(" -> ");
                row.nodeMapArr = data;
              }
            });

            csvContent += [
              headers.map((header) => header.text).join(","),
              ...arrData.map((item) => Object.values(item).join(",")),
            ].join("\n");

            const data = encodeURI(csvContent);
            const link = document.createElement("a");
            link.setAttribute("href", data);
            link.setAttribute(
              "download",
              "Part Where Used Report " + this.search + ".csv"
            );
            link.click();
          },
        },
      });
    },
  };
  return myWidget;
});

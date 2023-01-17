define("LCD/LCD_Part_Where_Used/assets/scripts/main", [
  "vue",
  "LCD/LCDLIB/scripts/vuetify.v2.6.13.min",
  "DS/WAFData/WAFData",
  "DS/PlatformAPI/PlatformAPI",
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
  Part_Where_Used_nls,
  Part_Where_Used_webservicedetails
) {
  Vue.use(Vuetify, {});
  var myWidget = {
    _3dashboardUrl: widget.widgetDomain,
    _3dspaceUrl: "",
    onLoad: function () {
      var apps = PlatformAPI.getAllApplicationConfigurations();
      for (var i = 0; i < apps.length; i++) {
        if (apps[i]["propertyKey"] === "app.urls.myapps") {
          var u = new URL(apps[i]["propertyValue"]);
          _3dspaceUrl = u.href;
          break;
        }
      }
      new Vue({
        el: "#app",
        vuetify: new Vuetify(),
        template: `
        <v-app>
        <v-container>
          <h2 class="title">
            Part - Where Used
          </h2>
          <v-snackbar v-model="snackbar">
            {{ snackbartext }}
    
            <template v-slot:action="{ attrs }">
              <v-btn color="red" text v-bind="attrs" @click="snackbar = false">
                Close
              </v-btn>
            </template>
          </v-snackbar>
          <v-card-title>
            <span class="h1 me-4">Part:</span>
    
            <v-text-field
              v-model="search"
              label="Title"
              single-line
              hide-details
              outlined
              @keydown.enter="sendRequest()"
              class="rounded-lg"
            ></v-text-field>
    
            <v-btn icons depressed rounded color="white" @click="sendRequest()">
              <v-icon color="black">
                mdi-magnify
              </v-icon>
            </v-btn>
          </v-card-title>
    
          <v-data-table
            :headers="header"
            :items="filteredItems"
            :items-per-page="5"
            class="elevation-1 rounded-lg"
            :loading="loadingStatus"
          >
            <!-- <template v-slot:header="{ headers }">
              <td v-for="h in headers">
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
              </td>
            </template> -->
    
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
              <span v-for="(i,index) in item.nodeMapArr" :key="i.PartId">
                <a target="_blank" :href="i.PartId">
                  {{ i.PartTitle }}
                </a>
                <span v-if="item.nodeMapArr.length > index + 1">
                  {{ " → " }}
                </span>
              </span>
              <span v-if="item.nodeMapArr.length == 0">
                Not Present 
              </span>
            </template>
          </v-data-table>
        </v-container>
      </v-app>
            `,
        data() {
          return {
            search: "",
            searchLevel: "",
            searchTopNodeName: "",
            searchTopNodeTitle: "",
            searchNavigation: "",
            header: Part_Where_Used_nls.headers,
            result: [],
            loadingStatus: false,
            snackbar: false,
            snackbartext: "",
          };
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
          sendRequest() {
            if (this.search.trim().length > 0) {
              this.result = [];
              this.loadingStatus = true;
              let reqNo = 0;
              let bomDataArr = widget.getValue("bomData");
              bomDataArr.forEach((bomData) => {
                let message = {
                  partTitle: this.search.trim(), //V21-B01030-00
                  bomData: bomData,
                };
                let _this = this;
                reqNo += 1;
                WAFData.authenticatedRequest(
                  _3dspaceUrl +
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
            } else {
              this.snackbar = true;
              this.snackbartext = "Please enter part title";
            }
          },
          // getArrows(nodeMapArr, nodeMap) {
          //   if (nodeMapArr.length > 0) {
          //     return nodeMap.sPartId !==
          //       nodeMapArr[nodeMapArr.length - 1].sPartId
          //       ? " → "
          //       : "";
          //   }
          // },
          formatUrl(data) {
            // let _3dashboardUrl =
            //   "https://sjc01en3apapd01.corp.lucid.lcl:446/3DDashboard";
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
            // return item.nodeMapArr
            //   .toLowerCase()
            //   .includes(this.searchNavigation.toLowerCase());

            let filterData = "Not Present";
            if (item.nodeMapArr.length > 0) {
              filterData = item.nodeMapArr.reduce(
                (one, two) => (one += two.PartTitle + ","),
                ""
              );
            }
            return filterData
              .toLowerCase()
              .includes(this.searchNavigation.toLowerCase());
          },
        },
      });
    },
  };
  return myWidget;
});

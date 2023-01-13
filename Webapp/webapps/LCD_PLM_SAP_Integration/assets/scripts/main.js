define("LCD/LCD_PLM_SAP_Integration/assets/scripts/main", [
  "vue",
  "DS/PlatformAPI/PlatformAPI",
  "DS/WAFData/WAFData",
  "LCD/LCD_PLM_SAP_Integration/lib/scripts/vuetify.min",
  "i18n!LCD/LCD_PLM_SAP_Integration/nls/PLM_SAP_Integration_nls",
  "css!LCD/LCD_PLM_SAP_Integration/lib/styles/vuetify.min.css",
  "css!LCD/LCDLIB/styles/google.css",
  "css!LCD/LCDLIB/styles/materialdesignicons.min.css",
  "css!LCD/LCD_PLM_SAP_Integration/assets/styles/style.css",
], function (Vue, PlatformAPI, WAFData, Vuetify, PLM_SAP_Integration_nls) {
  Vue.use(Vuetify, {});
  var myWidget = {
    jsonResponse: "",
    onLoad: function () {
      // console.log("On Load call!!");
      var apps = PlatformAPI.getAllApplicationConfigurations();
      	for (var i = 0; i < apps.length; i++) {
      		if (apps[i]["propertyKey"] === "app.urls.myapps") {
      			var u = new URL(apps[i]["propertyValue"]);
      			_3dspaceUrl = u.href;
      			break;
      		}
      	}
        // console.log("3DSPACE " +_3dspaceUrl);
      myWidget.webserviceToGetAllBOMComponents();
      // myWidget.setVueTemplate();
      myWidget.loadData();
    },
    // setVueTemplate: () => {
    //   var $body = $(widget.body);
    //   // var vueTags = `<div id='app'>
    //   $body.html(vueTags);
    // },
    webserviceToGetAllBOMComponents: function () {
      var _this = this;
      // WAFData.authenticatedRequest(_3dspaceUrl + '/LCDSAPIntegrationModeler/LCDSAPIntegrationService/getMA',
      WAFData.authenticatedRequest(_3dspaceUrl + widget.getValue("webserviceURLToGetAllBOMComponents"),
        {
          method: "GET",
          accept: "application/json",
          onComplete: function (dataResp) {
            var data = JSON.parse(dataResp);
            _this.vueapp.BOMComponentsReceivedFromWS = data;
            _this.vueapp.arrFilterData = data;
            
          },
          onFailure: function (error) {
            console.log(error);
          },
        }
      );
    },
    webserviceForRepush: async function () {
      debugger;
      var _this = this;
    
      for (let i = 0; i < _this.vueapp.selected.length; i++) {
        var Ca_ID = _this.vueapp.selected[i].caID;
        var BOM_Comp_ID = _this.vueapp.selected[i].BOMComponentID;
        var ma_name = _this.vueapp.selected[i].BOMComponentName;
        var Connection_ID = _this.vueapp.selected[i].ConnectionID;
        var data_to_be_Send_from_UI = {
          CAID: Ca_ID,
          BOMComponentID: BOM_Comp_ID,
          ConnectionID: Connection_ID,
          BOMName: ma_name,
        };
       await myWidget
          .methodToCallRepushWS(data_to_be_Send_from_UI)
          .then(function () {
            //_this.vueapp.snackbarMsg = [myWidget.jsonResponse];
            // var x = JSON.stringify(myWidget.jsonResponse);
            console.log(myWidget.jsonResponse);
           _this.vueapp.snackbarMsg.push(myWidget.jsonResponse);
          //  _this.vueapp.snackbarMsg.push(myWidget.jsonResponse[i]);

            // _this.vueapp.snackbarMsg.push(_this.vueapp.selected[i].BOMComponentName);
            // console.log("MYJSON -->" + myWidget.jsonResponse);
            if (_this.vueapp.snackbarMsg[i].status == "Success") {
              _this.vueapp.snackbarColor = "success";
              _this.vueapp.snackbarIcon = "mdi-check-circle";
              _this.vueapp.snackbar = true;
              _this.vueapp.SearchMethod.map((x) => {
                _this.vueapp.selected.map((y) => {
                  if (x.BOMComponentID == y.BOMComponentID) {
                    x.SapFeedbackMessage =
                      PLM_SAP_Integration_nls.rePushWebserviceResponceMessage_success;
                  }
                });
              });
            } else if (_this.vueapp.snackbarMsg[i].status == "Failed") {
              _this.vueapp.snackbarColor = "error";
              _this.vueapp.snackbarIcon = "mdi-close-circle";
              // _this.vueapp.snackbarMsg.push(returnData);
              _this.vueapp.snackbar = true;
              _this.vueapp.SearchMethod.map((x) => {
                _this.vueapp.selected.map((y) => {
                  if (x.BOMComponentID == y.BOMComponentID) {
                    x.status = PLM_SAP_Integration_nls.failed;
                    x.SapFeedbackMessage =
                      PLM_SAP_Integration_nls.rePushWebserviceResponceMessage_failed;
                  }
                });
              });
            }
          })
          .catch(function () {
            // alert("ERROR OCCURED!!!");
            _this.vueapp.snackbarColor = "error";
            _this.vueapp.snackbarIcon = "mdi-close-circle";
            _this.vueapp.snackbarMsg.push("Something went wrong.");
            // console.log(_this.vueapp.snackbarMsg);
            _this.vueapp.snackbar = true;

            _this.vueapp.SearchMethod.map((x) => {
              _this.vueapp.selected.map((y) => {
                if (x.BOMComponentID == y.BOMComponentID) {
                  x.status = PLM_SAP_Integration_nls.failed;
                  x.SapFeedbackMessage =
                    PLM_SAP_Integration_nls.rePushWebserviceResponceMessage_failed;
                }
              });
            });
          });
      }
      _this.vueapp.selected = [];
      // _this.vueapp.snackbarMsg = [];
    },
    methodToCallRepushWS: function (data_to_be_Send_from_UI) {
      return new Promise(function (resolve, reject) {
        try {
          WAFData.authenticatedRequest(
            _3dspaceUrl + widget.getValue("webserviceURLForRepush"),
            {
              method: "POST",
              accept: "application/json",
              crossOrigin: true,
              timeout: 7000,
              data: JSON.stringify(data_to_be_Send_from_UI),
              headers: {
                "Content-Type": "application/json",
              },
              onComplete: function (myJson) {
                // alert("ONLOAD CALLED!!!");
                myWidget.jsonResponse = JSON.parse(myJson);
                resolve();
              },
              onFailure: function (error) {
                console.log( "--------> ERROR ON WEBSERVICE FAILURE " +error);
                reject();
              },
            }
          );
        } catch (err) {
          console.log("------>>> ERROR CATCH :" +err);
        }
      });
    },
    webserviceToExportPartData: function () {
      var _this = this;
      var Ca_ID = _this.vueapp.selected[0].caID;
      var BOM_Comp_ID = _this.vueapp.selected[0].BOMComponentID;
      var Connection_ID = _this.vueapp.selected[0].ConnectionID;
      var BOM_Comp_Name = _this.vueapp.selected[0].BOMComponentName;
      var dataTobeSendfromUI = {
        CAID: Ca_ID,
        BOMComponentID: BOM_Comp_ID,
        ConnectionID: Connection_ID,
        BOMComponentName : BOM_Comp_Name
      };
      WAFData.authenticatedRequest(
        _3dspaceUrl + widget.getValue("webserviceURLToExportPartData"),
        {
          method: "POST",
          accept: "application/json",
          crossOrigin: true,
          data: JSON.stringify(dataTobeSendfromUI),
          headers: {
            "Content-Type": "application/json",
          },
          onComplete: function (dataResp) {
            // var data = JSON.parse(dataResp);
            // _this.vueapp.partData = data;
            // console.table("RESPONCE ------>" + dataResp);
            // var headers = dataResp.name;
            // alert(headers);
            _this.vueapp.download_csv_of_tabledata(
              dataResp,BOM_Comp_Name
            );
          },
          onFailure: function (error) {
            alert("ERROR OCCURED!!!");
            console.log(error);
          },
        }
      );
      _this.vueapp.selected = [];
    },

    loadData: function () {
      var vueapp = new Vue({
        el: "#app",
        vuetify: new Vuetify(),
        template : `
        <div id="app">
        <v-app id="inspire">
      <p style="text-align: right;">Last refreshed on: {{ date }} {{ time }}</p>
      <div id="title">
        <h1>3DX-SAP Integration</h1>
      </div>
      <v-container>
      <div class="snackbar_div">
        <v-snackbar v-model="snackbar" v-for="item in snackbarMsg" :key="item" :color="snackbarColor" top right :timeout="4000">
          <p><v-icon>{{snackbarIcon}}</v-icon>Name : {{item.BOMName}}<br><span class="space">Status : {{item.status}}</span></p>
          <template v-slot:action="{ attrs }">
          <v-btn
            text
            color="white"
            v-bind="attrs"
            @click="snackbar = false"
          >
          <v-icon>mdi-close</v-icon>
          </v-btn>
        </template>
        </v-snackbar>
      </div>
      </v-container>
        <div id="tabdiv">
          <v-tabs
            id="tabs"
            text
            v-model="model"
            centered
            hide-slider
            color="black"
          >
            <v-tab :href="'#tab-All'" active-class="teal lighten-2">
              All
            </v-tab>
            <v-tab :href="'#tab-Waiting'" active-class="yellow lighten-2">
              Waiting
            </v-tab>
            <v-tab :href="'#tab-InWork'" active-class="blue lighten-2">
              Inwork
            </v-tab>
            <v-tab :href="'#tab-Complete'" active-class="green lighten-2">
              Complete
            </v-tab>
            <v-tab :href="'#tab-Failed'" active-class="red lighten-2">
              Failed
            </v-tab>
          </v-tabs>
        </div>
        <v-tabs-items v-model="model">
          <v-tab-item
            :value="'tab-Waiting'"
            :transition="false"
            :reverse-transition="false"
          >
            <v-sheet class="overflow-y-auto" max-height="800">
              <v-app-bar class="ma-5" color="white" flat>
                  <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_table_to_csv_method"
                    >Export table to CSV
                  </v-btn>
                  <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_part_data"
                    :disabled="!(selected.length == 1)"
                    >Export part data to CSV
                  </v-btn>
                <v-spacer></v-spacer>
                <v-text-field
                class="globalsearch"
                  id="searchfield"
                  v-model="globalSearch"
                  label="Search..."
                  color="blue"
                  clear-icon="mdi-close-circle"
                  placeholder="Search..."
                  rounded
                  outlined
                  centered
                  clearable
                  @click:clear="clear"
                >
                </v-text-field>
                <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="searchTable"
                      id="searchbtn"
                      >Search
                    </v-btn>
              </v-app-bar>
              <v-container fluid style="height: 1000px; overflow: auto;">
                <v-data-table
                  v-model="selected"
                  :headers="headersForSeperateTab"
                  :items="methodToAddSrNoInWaitingTable"
                  item-key="BOMComponentName"
                  :search="search"
                  fixed-header
                  height="600px"
                  show-select
                  class="elevation-1"
                  elevation="8"
                  checkbox-color="teal lighten-2"
                  must-sort
                >
                  <template v-slot:header.BOMComponentName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="BOMComponentName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="BOMComponentName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="BOMComponentName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.status="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="status ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="status"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="status = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.revision="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="revision ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="revision"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="revision = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.title="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="title ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="title"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="title = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.maturity="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="maturity ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="maturity"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="maturity = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.description="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="description ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="description"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="description = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caCompletedTime="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caCompletedTime ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caCompletedTime"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caCompletedTime = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackTimeStamp="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false" >
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="SapFeedbackTimeStamp ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px" >
                      <v-text-field
                        v-model="SapFeedbackTimeStamp"
                        class="pa-4 "
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="SapFeedbackTimeStamp = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                      >Clear</v-btn>
                    </div>
                  </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackMessage="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false" >
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="SapFeedbackMessage ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px" >
                      <v-text-field
                        v-model="SapFeedbackMessage"
                        class="pa-4 "
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="SapFeedbackMessage = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                      >Clear</v-btn>
                    </div>
                  </v-menu>
                  </template>
                </v-data-table>
              </v-container>
            </v-sheet>
          </v-tab-item>
        </v-tabs-items>
        <v-tabs-items v-model="model">
          <v-tab-item
            :value="'tab-InWork'"
            :transition="false"
            :reverse-transition="false"
          >
            <v-sheet class="overflow-y-auto" max-height="800">
              <v-app-bar class="ma-5" color="white" flat>
              <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_table_to_csv_method"
                    >Export table to CSV
                  </v-btn>
                  <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_part_data"
                    :disabled="!(selected.length == 1)"
                    >Export part data to CSV
                  </v-btn>
                <v-spacer></v-spacer>
                <v-text-field
                class="globalsearch"
                  id="searchfield"
                  v-model="globalSearch"
                  label="Search..."
                  color="blue"
                  clear-icon="mdi-close-circle"
                  placeholder="Search..."
                  rounded
                  outlined
                  centered
                  clearable
                  @click:clear="clear"
                >
                </v-text-field>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="searchTable"
                      id="searchbtn"
                      >
                      Search
                    </v-btn>
              </v-app-bar>
              <v-container fluid style="height: 1000px; overflow: auto;">
                <v-data-table
                  v-model="selected"
                  :headers="headersForSeperateTab"
                  :items="methodToAddSrNoInInWorkTable"
                  item-key="BOMComponentName"
                  :search="search"
                  fixed-header
                  height="600px"
                  show-select
                  class="elevation-1"
                  elevation="8"
                  checkbox-color="teal lighten-2"
                  must-sort
                >
                  <template v-slot:header.BOMComponentName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="BOMComponentName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="BOMComponentName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="BOMComponentName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.status="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="status ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="status"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="status = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.revision="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="revision ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="revision"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="revision = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.title="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="title ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="title"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="title = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.maturity="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="maturity ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="maturity"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="maturity = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.description="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="description ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="description"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="description = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caCompletedTime="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caCompletedTime ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caCompletedTime"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caCompletedTime = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackTimeStamp="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackTimeStamp ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackTimeStamp"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackTimeStamp = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
          <template v-slot:header.SapFeedbackMessage="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackMessage ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackMessage"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackMessage = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
                </v-data-table>
              </v-container>
            </v-sheet>
          </v-tab-item>
        </v-tabs-items>
        <v-tabs-items v-model="model">
          <v-tab-item
            :value="'tab-Complete'"
            :transition="false"
            :reverse-transition="false"
          >
            <v-sheet class="overflow-y-auto" max-height="800">
              <v-app-bar class="ma-5" color="white" flat>
              <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_table_to_csv_method"
                    >Export table to CSV
                  </v-btn>
                  <v-btn
                    depressed
                    color="primary"
                    class="buttons"
                    @click="export_part_data"
                    :disabled="!(selected.length == 1)"
                    >Export part data to CSV
                  </v-btn>
                <v-spacer></v-spacer>
                <v-text-field
                class="globalsearch"
                  id="searchfield"
                  v-model="globalSearch"
                  label="Search..."
                  color="blue"
                  clear-icon="mdi-close-circle"
                  placeholder="Search..."
                  rounded
                  centered
                  outlined
                  clearable
                  @click:clear="clear"
                >
                </v-text-field>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="searchTable"
                      id="searchbtn"
                      >Search
                    </v-btn>
              </v-app-bar>
              <v-container fluid style="height: 1000px; overflow: auto;">
                <v-data-table
                  v-model="selected"
                  :headers="headersForSeperateTab"
                  :items="methodToAddSrNoInCompleteTable"
                  item-key="BOMComponentName"
                  :search="search"
                  fixed-header
                  height="600px"
                  show-select
                  class="elevation-1"
                  elevation="8"
                  checkbox-color="teal lighten-2"
                  must-sort
                >
                  <template v-slot:header.BOMComponentName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="BOMComponentName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="BOMComponentName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="BOMComponentName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.status="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="status ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="status"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="status = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.revision="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="revision ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="revision"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="revision = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.title="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="title ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="title"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="title = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.maturity="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="maturity ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="maturity"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="maturity = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  
                  <template v-slot:header.description="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="description ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="description"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="description = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caCompletedTime="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caCompletedTime ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caCompletedTime"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caCompletedTime = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackTimeStamp="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackTimeStamp ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackTimeStamp"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackTimeStamp = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
          <template v-slot:header.SapFeedbackMessage="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackMessage ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackMessage"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackMessage = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
                </v-data-table>
              </v-container>
            </v-sheet>
            <!-- </div> -->
          </v-tab-item>
        </v-tabs-items>
        <v-tabs-items v-model="model">
          <v-tab-item
            :value="'tab-Failed'"
            :transition="false"
            :reverse-transition="false"
          >
            <v-sheet class="overflow-y-auto" max-height="800">
              <v-app-bar class="ma-5" color="white" flat>
                    <v-btn class="buttons" @click="rePushToSAPMethod"
                    depressed
                    color="primary"
                    :disabled="!failedStatus">
                    Re push to SAP
                    </v-btn>
                <v-btn
                depressed
                color="primary"
                class="buttons"
                @click="export_table_to_csv_method"
                >Export table to CSV
              </v-btn>
              <v-btn
                depressed
                color="primary"
                class="buttons"
                @click="export_part_data"
                :disabled="!(selected.length == 1)"
                >Export part data to CSV
              </v-btn>
                <v-spacer></v-spacer>
                <v-text-field
                class="globalsearch"
                  id="searchfield"
                  v-model="globalSearch"
                  label="Search..."
                  color="blue"
                  clear-icon="mdi-close-circle"
                  placeholder="Search..."
                  rounded
                  centered
                  outlined
                  clearable
                  @click:clear="clear"
                >
                </v-text-field>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="searchTable"
                      id="searchbtn"
                      >
                      Search
                    </v-btn>
                  </span>
                </v-tooltip>
              </v-app-bar>
              <v-container fluid style="height: 1000px; overflow: auto;">
                <v-data-table
                  v-model="selected"
                  :headers="headersForSeperateTab"
                  :items="methodToAddSrNoInFailedTable"
                  item-key="BOMComponentName"
                  :search="search"
                  fixed-header
                  height="600px"
                  show-select
                  elevation="8"
                  checkbox-color="teal lighten-2"
                  must-sort
                  @item-selected="methodToDisableRepushBtnOnSelect"
                  @toggle-select-all="methodToDisableRepushBtnOnSelectAll"
                >
                  <template v-slot:header.BOMComponentName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="BOMComponentName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="BOMComponentName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="BOMComponentName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.status="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="status ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="status"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="status = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.revision="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="revision ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="revision"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="revision = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.title="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="title ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="title"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="title = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.maturity="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="maturity ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="maturity"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="maturity = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.description="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="description ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="description"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="description = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caCompletedTime="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caCompletedTime ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caCompletedTime"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caCompletedTime = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.caName="{ header }">
                    {{ header.text }}
                    <v-menu offset-y :close-on-content-click="false">
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon v-bind="attrs" v-on="on">
                          <v-icon small :color="caName ? 'primary' : ''">
                            mdi-magnify-expand
                          </v-icon>
                        </v-btn>
                      </template>
                      <div style="background-color: white; width: 280px">
                        <v-text-field
                          v-model="caName"
                          class="pa-4"
                          type="text"
                          label="Enter the search term"
                          :autofocus="true"
                        ></v-text-field>
                        <v-btn
                          @click="caName = ''"
                          small
                          text
                          color="primary"
                          class="ml-2 mb-2"
                          >Clear</v-btn
                        >
                      </div>
                    </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackTimeStamp="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false" >
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="SapFeedbackTimeStamp ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px" >
                      <v-text-field
                        v-model="SapFeedbackTimeStamp"
                        class="pa-4 "
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="SapFeedbackTimeStamp = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                      >Clear</v-btn>
                    </div>
                  </v-menu>
                  </template>
                  <template v-slot:header.SapFeedbackMessage="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false" >
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="SapFeedbackMessage ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px" >
                      <v-text-field
                        v-model="SapFeedbackMessage"
                        class="pa-4 "
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="SapFeedbackMessage = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                      >Clear</v-btn>
                    </div>
                  </v-menu>
                  </template>
                </v-data-table>
              </v-container>
            </v-sheet>
            <!-- </div> -->
          </v-tab-item>
        </v-tabs-items>
        <v-tabs-items v-model="model">
          <v-tab-item
            :value="'tab-All'"
            :transition="false"
            :reverse-transition="false"
          >
            <v-sheet class="overflow-y-auto" max-height="800">
              <v-app-bar class="ma-5" color="white" flat>
                    <v-btn class="buttons" @click="rePushToSAPMethod"
                    color="primary"
                    depressed
                    :disabled="!failedStatus">
                    Re-push to SAP
                    </v-btn>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="export_table_to_csv_method"
                      >Export table to CSV
                    </v-btn>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="export_part_data"
                      :disabled="!(selected.length == 1)"
                      >Export part data to CSV
                    </v-btn>
                <v-spacer></v-spacer>
                <v-text-field
                class="globalsearch"
                  id="searchfield"
                  v-model="globalSearch"
                  label="Search..."
                  color="blue"
                  clear-icon="mdi-close-circle"
                  placeholder="Search..."
                  rounded
                  outlined
                  centered
                  clearable
                  @click:clear="clear"
                  @keydown.enter="searchTable"
                >
                </v-text-field>
                    <v-btn
                      depressed
                      color="primary"
                      class="buttons"
                      @click="searchTable"
                      id="searchbtn"
                      >Search
                    </v-btn>
               
              </v-app-bar>
              <v-container fluid style="height: 1000px; overflow: auto;">
              <v-data-table
              display: block
              v-model="selected"
              :headers="headersForAllInOneTab"
              :items="methodToAddSrNoInAllTab"
              item-key="BOMComponentName"
              :search="search"
              fixed-header
              height= "600px"
              show-select
              elevation="8"
              checkbox-color="teal lighten-2"
              resizable="true"
              must-sort
              :custom-sort="methodtodoCustomSortinTable"
              @item-selected="methodToDisableRepushBtnOnSelect"
              @toggle-select-all="methodToDisableRepushBtnOnSelectAll"
            >
              <template v-slot:item.status="{ item }">
                <v-chip :color="getStatusColor(item.status)">
                  {{ item.status }}
                </v-chip>
              </template>
              <template v-slot:header.BOMComponentName="{ header }">
              {{ header.text }}
              <v-menu offset-y :close-on-content-click="false" >
                <template v-slot:activator="{ on, attrs }">
                  <v-btn icon v-bind="attrs" v-on="on">
                    <v-icon small :color="BOMComponentName ? 'primary' : ''">
                      mdi-magnify-expand
                    </v-icon>
                  </v-btn>
                </template>
                <div style="background-color: white; width: 280px" >
                  <v-text-field
                    v-model="BOMComponentName"
                    class="pa-4 "
                    type="text"
                    label="Enter the search term"
                    :autofocus="true"
                  ></v-text-field>
                  <v-btn
                    @click="BOMComponentName = ''"
                    small
                    text
                    color="primary"
                    class="ml-2 mb-2"
                  >Clear</v-btn>
                </div>
              </v-menu>
              </template>
      <template v-slot:header.status="{ header }">
      {{ header.text }}
      <v-menu offset-y :close-on-content-click="false" >
        <template v-slot:activator="{ on, attrs }">
          <v-btn icon v-bind="attrs" v-on="on">
            <v-icon small :color="status ? 'primary' : ''">
              mdi-magnify-expand
            </v-icon>
          </v-btn>
        </template>
        <div style="background-color: white; width: 280px" >
          <v-text-field
            v-model="status"
            class="pa-4 "
            type="text"
            label="Enter the search term"
            :autofocus="true"
          ></v-text-field>
          <v-btn
            @click="status = ''"
            small
            text
            color="primary"
            class="ml-2 mb-2"
          >Clear</v-btn>
        </div>
      </v-menu>
      </template>
      <template v-slot:header.revision="{ header }">
      {{ header.text }}
      <v-menu offset-y :close-on-content-click="false" >
        <template v-slot:activator="{ on, attrs }">
          <v-btn icon v-bind="attrs" v-on="on">
            <v-icon small :color="revision ? 'primary' : ''">
              mdi-magnify-expand
            </v-icon>
          </v-btn>
        </template>
        <div style="background-color: white; width: 280px" >
          <v-text-field
            v-model="revision"
            class="pa-4 "
            type="text"
            label="Enter the search term"
            :autofocus="true"
          ></v-text-field>
          <v-btn
            @click="revision = ''"
            small
            text
            color="primary"
            class="ml-2 mb-2"
          >Clear</v-btn>
        </div>
      </v-menu>
      </template>
      <template v-slot:header.title="{ header }">
        {{ header.text }}
        <v-menu offset-y :close-on-content-click="false" >
          <template v-slot:activator="{ on, attrs }">
            <v-btn icon v-bind="attrs" v-on="on">
              <v-icon small :color="title ? 'primary' : ''">
                mdi-magnify-expand
              </v-icon>
            </v-btn>
          </template>
          <div style="background-color: white; width: 280px" >
            <v-text-field
              v-model="title"
              class="pa-4 "
              type="text"
              label="Enter the search term"
              :autofocus="true"
            ></v-text-field>
            <v-btn
              @click="title = ''"
              small
              text
              color="primary"
              class="ml-2 mb-2"
            >Clear</v-btn>
          </div>
        </v-menu>
      </template>
            <template v-slot:header.maturity="{ header }">
            {{ header.text }}
            <v-menu offset-y :close-on-content-click="false" >
              <template v-slot:activator="{ on, attrs }">
                <v-btn icon v-bind="attrs" v-on="on">
                  <v-icon small :color="maturity ? 'primary' : ''">
                    mdi-magnify-expand
                  </v-icon>
                </v-btn>
              </template>
              <div style="background-color: white; width: 280px" >
                <v-text-field
                  v-model="maturity"
                  class="pa-4 "
                  type="text"
                  label="Enter the search term"
                  :autofocus="true"
                ></v-text-field>
                <v-btn
                  @click="maturity = ''"
                  small
                  text
                  color="primary"
                  class="ml-2 mb-2"
                >Clear</v-btn>
                </div>
              </v-menu>
            </template>
            <template v-slot:header.description="{ header }">
            {{ header.text }}
            <v-menu offset-y :close-on-content-click="false" >
              <template v-slot:activator="{ on, attrs }">
                <v-btn icon v-bind="attrs" v-on="on">
                  <v-icon small :color="description ? 'primary' : ''">
                    mdi-magnify-expand
                  </v-icon>
                </v-btn>
              </template>
              <div style="background-color: white; width: 280px" >
                <v-text-field
                  v-model="description"
                  class="pa-4 "
                  type="text"
                  label="Enter the search term"
                  :autofocus="true"
                ></v-text-field>
                <v-btn
                  @click="description = ''"
                  small
                  text
                  color="primary"
                  class="ml-2 mb-2"
                >Clear</v-btn>
              </div>
            </v-menu>
          </template>
          <template v-slot:header.caCompletedTime="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="caCompletedTime ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="caCompletedTime"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="caCompletedTime = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
          <template v-slot:header.caName="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="caName ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="caName"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="caName = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
          <template v-slot:header.SapFeedbackTimeStamp="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackTimeStamp ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackTimeStamp"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackTimeStamp = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
          <template v-slot:header.SapFeedbackMessage="{ header }">
          {{ header.text }}
          <v-menu offset-y :close-on-content-click="false" >
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on">
                <v-icon small :color="SapFeedbackMessage ? 'primary' : ''">
                  mdi-magnify-expand
                </v-icon>
              </v-btn>
            </template>
            <div style="background-color: white; width: 280px" >
              <v-text-field
                v-model="SapFeedbackMessage"
                class="pa-4 "
                type="text"
                label="Enter the search term"
                :autofocus="true"
              ></v-text-field>
              <v-btn
                @click="SapFeedbackMessage = ''"
                small
                text
                color="primary"
                class="ml-2 mb-2"
              >Clear</v-btn>
            </div>
          </v-menu>
          </template>
            </v-data-table>
              </v-container>
            </v-sheet>
          </v-tab-item>
        </v-tabs-items>
      <v-footer app>
        Lucid 3DX-SAP Integration Dashboard
      </v-footer>
    </v-app>
    </div>`,
        data: {
          snackbarColor: "success",
          snackbarMsg: [],
          snackbarIcon: "",
          arrObj: [],
          failedStatus: false,
          snackbar: false,
          type: 1,
          // active: true,
          BOMComponentName: "",
          status: "",
          revision: "",
          title: "",
          maturity: "",
          description: "",
          caCompletedTime: "",
          caName: "",
          SapFeedbackTimeStamp: "",
          SapFeedbackMessage: "",
          arrFilterData: [],
          model: null,
          singleSelect: false,
          search: "",
          globalSearch: "",
          BOMComponentsReceivedFromWS: [],
          selected: [],
          headersForAllInOneTab: PLM_SAP_Integration_nls.headersForAllInOneTab,
          headersForSeperateTab: PLM_SAP_Integration_nls.headersForSeperateTab,
          time: "",
          date: "",
        },
        computed: {
          // methodForGettingDataForRepush(){
          //   const a = this.selected.map((obj) => [obj.BOMComponentName, obj.BOMComponentID, obj.caID, obj.ConnectionID]);
          //   this.maDetails = a;
          //   console.log(a);
          // },
          // checkbox method to enable only failed ones
          // onlyFailed() {
          // return this.SearchMethod.map(x => ({
          //   ...x,
          //   isSelectable: x.status == PLM_SAP_Integration_nls.failed
          // }))
          // },
          methodToAddSrNoInAllTab() {
            return this.SearchMethod.map((d, index) => ({
              ...d,
              sno: index + 1,
            }));
          },
          methodToAddSrNoInWaitingTable() {
            return this.methodToGetWaitingTable.map((d, index) => ({
              ...d,
              sno: index + 1,
            }));
          },
          methodToAddSrNoInInWorkTable() {
            return this.methodToGetInWorkTable.map((d, index) => ({
              ...d,
              sno: index + 1,
            }));
          },
          methodToAddSrNoInCompleteTable() {
            return this.methodToGetSuccessTable.map((d, index) => ({
              ...d,
              sno: index + 1,
            }));
          },
          methodToAddSrNoInFailedTable() {
            return this.methodToGetFailedTable.map((d, index) => ({
              ...d,
              sno: index + 1,
            }));
          },
          SearchMethod() {
            let conditions = [];
           
            if (this.BOMComponentName) {
              conditions.push(this.filtermaName);
            }
            if (this.status) {
              conditions.push(this.filterStatus);
            }
            if (this.revision) {
              conditions.push(this.filterRevision);
            }
            if (this.title) {
              conditions.push(this.filterTitle);
            }
            if (this.maturity) {
              conditions.push(this.filterMaturity);
            }
            if (this.description) {
              conditions.push(this.filterDescription);
            }
            if (this.caCompletedTime) {
              conditions.push(this.filterCaCompletedTime);
            }
            if (this.caName) {
              conditions.push(this.filterCaName);
            }
            if (this.SapFeedbackTimeStamp) {
              conditions.push(this.filterSapFeedbackTimeStamp);
            }
            if (this.SapFeedbackMessage) {
              conditions.push(this.filterSapFeedbackMessage);
            }
            if (conditions.length > 0) {
              return this.arrFilterData.filter((name) => {
                return conditions.every((condition) => {
                  return condition(name);
                });
              });
            }
            return this.arrFilterData;
          },
          methodToGetSuccessTable() {
            return this.SearchMethod.filter(
              (x) => x.status == PLM_SAP_Integration_nls.success
            );
          },
          methodToGetFailedTable() {
            return this.SearchMethod.filter(
              (x) => x.status == PLM_SAP_Integration_nls.failed
            );
          },
          methodToGetWaitingTable() {
            return this.SearchMethod.filter(
              (x) => x.status == PLM_SAP_Integration_nls.waiting
            );
          },
          methodToGetInWorkTable() {
            return this.SearchMethod.filter(
              (x) => x.status == PLM_SAP_Integration_nls.inWork
            );
          },
        },
        methods: {
          methodToDisableRepushBtnOnSelectAll(obj1) {
            if (!obj1.value) {
              this.arrObj = [];
            }
            if (
              obj1.items.length ==
              obj1.items.filter(
                (x) => x.status == PLM_SAP_Integration_nls.failed
              ).length
            ) {
              this.failedStatus = true;
            } else {
              this.failedStatus = false;
            }
          },
          methodToDisableRepushBtnOnSelect(obj) {
            if (this.selected.length == this.SearchMethod.length) {
              this.arrObj = this.selected;
            }
            if (obj.value) {
              this.arrObj.push(obj.item);
            } else {
              var index = this.arrObj.indexOf(obj.item);
              this.arrObj.splice(index, 1);
              // arrobj = arrobj.filter(item => item !== obj);
            }
            if (
              this.arrObj.filter(
                (x) => x.status !== PLM_SAP_Integration_nls.failed
              ).length > 0
            ) {
              this.failedStatus = false;
            } else if (
              this.arrObj.filter(
                (x) => x.status == PLM_SAP_Integration_nls.failed
              ).length > 0
            ) {
              this.failedStatus = true;
            } else {
              this.failedStatus = false;
            }
          },
          methodtodoCustomSortinTable: function (items, index, isDesc) {
            items.sort((a, b) => {
              if (index[0] == PLM_SAP_Integration_nls.caCompletedTime) {
                if (!isDesc[0]) {
                  return new Date(b[index]) - new Date(a[index]);
                } else {
                  return new Date(a[index]) - new Date(b[index]);
                }
              } else if (index[0] == PLM_SAP_Integration_nls.sno) {
                if (!isDesc[0]) {
                  return b[index] - a[index];
                } else {
                  return a[index] - b[index];
                }
              } else {
                if (typeof a[index] != "undefined") {
                  if (!isDesc[0]) {
                    return a[index]
                      .toLowerCase()
                      .localeCompare(b[index].toLowerCase());
                  } else {
                    return b[index]
                      .toLowerCase()
                      .localeCompare(a[index].toLowerCase());
                  }
                }
              }
            });
            return items;
          },
          download_csv_of_tabledata(csv, filename) {
            var csvFile;
            var downloadLink;

            // CSV FILE
            csvFile = new Blob([csv], { type: "text/csv" });

            // Download link
            downloadLink = document.createElement("a");

            downloadLink.download = filename;

            downloadLink.href = window.URL.createObjectURL(csvFile);

            downloadLink.style.display = "none";

            document.body.appendChild(downloadLink);

            downloadLink.click();
          },
          export_table_to_csv_method(html, filename) {
            var _this = this;
            var csv = [];
            // var csv = this.selected;
            var rows = document.querySelectorAll("table tr");

            for (var i = 0; i < rows.length; i++) {
              var row = [],
                cols = rows[i].querySelectorAll("td, th");

              for (var j = 1; j < cols.length; j++) row.push(cols[j].innerText);

              csv.push(row.join(","));
            }
            // Download CSV
            // _this.download_csv_of_tabledata(csv.join("\n"), filename);
            _this.download_csv_of_tabledata(
              csv.join("\n"),
              "3DX-SAP Integration"
            );
          },
          export_part_data() {
            myWidget.webserviceToExportPartData();
            // _this.download_csv_of_tabledata(this.partData.join("\n"), "Sub contract part");
          },
          getStatusColor(status) {
            if (status === PLM_SAP_Integration_nls.success)
              return "green lighten-1";
            else if (status === PLM_SAP_Integration_nls.failed)
              return "red lighten-1";
            else if (status === PLM_SAP_Integration_nls.inWork)
              return "blue lighten-1";
            else if (status === PLM_SAP_Integration_nls.waiting)
              return "yellow lighten-1";
          },
          filtermaName(item) {
            return item.BOMComponentName.toLowerCase().includes(
              this.BOMComponentName.toLowerCase()
            );
          },
          filterStatus(item) {
            return item.status
              .toLowerCase()
              .includes(this.status.toLowerCase());
          },
          filterRevision(item) {
            return item.revision
              .toLowerCase()
              .includes(this.revision.toLowerCase());
          },
          filterTitle(item) {
            return item.title.toLowerCase().includes(this.title.toLowerCase());
          },
          filterMaturity(item) {
            return item.maturity
              .toLowerCase()
              .includes(this.maturity.toLowerCase());
          },
          filterDescription(item) {
            return item.description
              .toLowerCase()
              .includes(this.description.toLowerCase());
          },
          filterCaCompletedTime(item) {
            return item.caCompletedTime
              .toLowerCase()
              .includes(this.caCompletedTime.toLowerCase());
          },
          filterCaName(item) {
            return item.caName
              .toLowerCase()
              .includes(this.caName.toLowerCase());
          },
          filterSapFeedbackTimeStamp(item) {
            return item.SapFeedbackTimeStamp.toLowerCase().includes(
              this.SapFeedbackTimeStamp.toLowerCase()
            );
          },
          filterSapFeedbackMessage(item) {
            return item.SapFeedbackMessage.toLowerCase().includes(
              this.SapFeedbackMessage.toLowerCase()
            );
          },
          rePushToSAPMethod() {
            this.SearchMethod.map((x) => {
              this.selected.map((y) => {
                if (x.BOMComponentID == y.BOMComponentID) {
                  x.status = PLM_SAP_Integration_nls.inWork;
                }
              });
            });
            myWidget.webserviceForRepush();
            this.failedStatus = false;
            this.snackbarMsg = [];
          },
          clear() {
            this.arrFilterData = this.BOMComponentsReceivedFromWS;
          },
          searchTable() {
            if (this.globalSearch === "" || this.globalSearch === null) {
              this.arrFilterData = this.BOMComponentsReceivedFromWS;
            } else {
              return (this.arrFilterData =
                this.BOMComponentsReceivedFromWS.filter((data) => {
                  return (
                    data.BOMComponentName
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.status
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.revision
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.title
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.maturity
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.description
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.caCompletedTime
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.caName
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.SapFeedbackTimeStamp
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase()) ||
                    data.SapFeedbackMessage
                      .toLowerCase()
                      .includes(this.globalSearch.toLowerCase())
                  );
                }));
            }
          },
        },
        mounted() {
          // this.arrFilterData = this.BOMComponentsReceivedFromWS;
          // get a new date (locale machine date time)
          var date1 = new Date();
          const yyyy = date1.getFullYear();
          let mm = date1.getMonth() + 1; // Months start at 0!
          let dd = date1.getDate();
          if (dd < 10) dd = "0" + dd;
          if (mm < 10) mm = "0" + mm;
          const formattedToday = dd + "/" + mm + "/" + yyyy;
          // get the date as a string
          this.date = formattedToday;
          // get the time as a string
          this.time = date1.toLocaleTimeString();
        },
      });
      myWidget.vueapp = vueapp;
    },
    onRefresh: function () {
      myWidget.onLoad();
    },
  };
  return myWidget;
});

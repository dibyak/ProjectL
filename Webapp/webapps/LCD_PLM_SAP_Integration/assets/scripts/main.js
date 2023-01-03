define("LCD/LCD_PLM_SAP_Integration/assets/scripts/main", [
  "UWA/Drivers/jQuery",
  "vue",
  'DS/PlatformAPI/PlatformAPI',
  'DS/WAFData/WAFData',
  "LCD/LCD_PLM_SAP_Integration/lib/scripts/vuetify.min",
  "i18n!LCD/LCD_PLM_SAP_Integration/nls/PLM_SAP_Integration_nls",
  "css!LCD/LCD_PLM_SAP_Integration/lib/styles/vuetify.min.css",
  "css!LCD/LCDLIB/styles/google.css",
  "css!LCD/LCDLIB/styles/materialdesignicons.min.css",
  "css!LCD/LCD_PLM_SAP_Integration/assets/styles/style.css"
], function ($,Vue,PlatformAPI,WAFData, Vuetify, PLM_SAP_Integration_nls) {
  Vue.use(Vuetify, {});
  var myWidget = {
    onLoad: function () {
      // console.log("On Load call!!");
      // var apps = PlatformAPI.getAllApplicationConfigurations();							
			// 	for (var i = 0; i < apps.length; i++) {
			// 		if (apps[i]["propertyKey"] === "app.urls.myapps") {
			// 			var u = new URL(apps[i]["propertyValue"]);
			// 			_3dspaceUrl = u.href;
			// 			break;
			// 		}
			// 	}
      //   console.log(_3dspaceUrl);
       myWidget.webserviceToGetAllBOMComponents();
      myWidget.setVueTemplate();
      myWidget.loadData();
      // myWidget.webserviceForRepush();
    },
    setVueTemplate: () => {
      var $body = $(widget.body);
      var vueTags = `<div id='app'>
      <v-app id="inspire">
    <p style="text-align: right;">Last refreshed on: {{ date }} {{ time }}</p>
    <div id="title">
      <h1>3DX-SAP Integration</h1>
    </div>
    <v-container>
      <v-snackbar v-model="snackbar" v-for="item in snackbarMsg" :key="item" :color="snackbar_color" top right :timeout="4000">
        <p><v-icon>mdi-check-circle</v-icon>{{item}}</p>
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
          <v-tab :href="'#tab-5'" active-class="indigo lighten-2">
            All
          </v-tab>
          <v-tab :href="'#tab-1'" active-class="yellow lighten-2">
            Waiting
          </v-tab>
          <v-tab :href="'#tab-2'" active-class="blue lighten-2">
            Inwork
          </v-tab>
          <v-tab :href="'#tab-3'" active-class="green lighten-2">
            Complete
          </v-tab>
          <v-tab :href="'#tab-4'" active-class="red lighten-2">
            Failed
          </v-tab>
        </v-tabs>
      </div>
      <v-tabs-items v-model="model">
        <v-tab-item
          :value="'tab-1'"
          :transition="false"
          :reverse-transition="false"
        >
          <v-sheet class="overflow-y-auto" max-height="800">
            <v-app-bar class="ma-5" color="white" flat>
            <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_table_to_csv"
                    >
                    <v-icon>mdi-export-variant</v-icon>
                  </v-btn>
                </template>
                <span>Export table to CSV</span>
              </v-tooltip>
              <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  depressed
                  color="primary"
                  icon
                  class="buttons"
                  @click="export_part_data"
                  :disabled="!(selected.length == 1)"
                  >
                  <v-icon>mdi-file-export</v-icon>
                </v-btn>
              </template>
              <span>Export part data to CSV</span>
            </v-tooltip>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="searchTable"
                    id="searchbtn"
                    >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </template>
                <span>Search</span>
              </v-tooltip>
            </v-app-bar>
            <v-container fluid style="height: 1500px; overflow: auto;">
              <v-data-table
                v-model="selected"
                :headers="headers"
                :items="itemsWithSno1"
                item-key="maName"
                :search="search"
                hide-default-footer
                fixed-header
                height="auto"
                show-select
                class="elevation-1"
                elevation="8"
                checkbox-color="teal lighten-2"
                must-sort
              >
                <template v-slot:header.maName="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false">
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="maName ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px">
                      <v-text-field
                        v-model="maName"
                        class="pa-4"
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
                      >
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
          :value="'tab-2'"
          :transition="false"
          :reverse-transition="false"
        >
          <v-sheet class="overflow-y-auto" max-height="800">
            <v-app-bar class="ma-5" color="white" flat>
            <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_table_to_csv"
                    >
                    <v-icon>mdi-export-variant</v-icon>
                  </v-btn>
                </template>
                <span>Export table to CSV</span>
              </v-tooltip>
              <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  depressed
                  color="primary"
                  icon
                  class="buttons"
                  @click="export_part_data"
                  :disabled="!(selected.length == 1)"
                  >
                  <v-icon>mdi-file-export</v-icon>
                </v-btn>
              </template>
              <span>Export part data to CSV</span>
            </v-tooltip>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="searchTable"
                    id="searchbtn"
                    >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </template>
                <span>Search</span>
              </v-tooltip>
            </v-app-bar>
            <v-container fluid style="height: 1500px; overflow: auto;">
              <v-data-table
                v-model="selected"
                :headers="headers"
                :items="itemsWithSno2"
                item-key="maName"
                :search="search"
                hide-default-footer
                fixed-header
                height="auto"
                show-select
                class="elevation-1"
                elevation="8"
                checkbox-color="teal lighten-2"
                must-sort
              >
                <template v-slot:header.maName="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false">
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="maName ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px">
                      <v-text-field
                        v-model="maName"
                        class="pa-4"
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
                      >
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
          :value="'tab-3'"
          :transition="false"
          :reverse-transition="false"
        >
          <v-sheet class="overflow-y-auto" max-height="800">
            <v-app-bar class="ma-5" color="white" flat>
            <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_table_to_csv"
                    >
                    <v-icon>mdi-export-variant</v-icon>
                  </v-btn>
                </template>
                <span>Export table to CSV</span>
              </v-tooltip>
              <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  depressed
                  color="primary"
                  icon
                  class="buttons"
                  @click="export_part_data"
                  :disabled="!(selected.length == 1)"
                  >
                  <v-icon>mdi-file-export</v-icon>
                </v-btn>
              </template>
              <span>Export part data to CSV</span>
            </v-tooltip>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="searchTable"
                    id="searchbtn"
                    >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </template>
                <span>Search</span>
              </v-tooltip>
            </v-app-bar>
            <v-container fluid style="height: 1500px; overflow: auto;">
              <v-data-table
                v-model="selected"
                :headers="headers"
                :items="itemsWithSno3"
                item-key="maName"
                :search="search"
                hide-default-footer
                fixed-header
                height="auto"
                show-select
                class="elevation-1"
                elevation="8"
                checkbox-color="teal lighten-2"
                must-sort
              >
                <template v-slot:header.maName="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false">
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="maName ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px">
                      <v-text-field
                        v-model="maName"
                        class="pa-4"
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
                      >
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
          :value="'tab-4'"
          :transition="false"
          :reverse-transition="false"
        >
          <v-sheet class="overflow-y-auto" max-height="800">
            <v-app-bar class="ma-5" color="white" flat>
            <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn class="buttons" @click="rePush"
                  depressed
                  color="primary"
                  icon
                  :disabled="!failedStatus"
                  v-bind="attrs"
                  v-on="on">
                  <v-icon>mdi-arrow-u-up-right-bold</v-icon>
                  </v-btn>
                </template>
                <span>Re-Push to SAP</span>
              </v-tooltip>
            <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_table_to_csv"
                    >
                    <v-icon>mdi-export-variant</v-icon>
                  </v-btn>
                </template>
                <span>Export table to CSV</span>
              </v-tooltip>
              <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  depressed
                  color="primary"
                  icon
                  class="buttons"
                  @click="export_part_data"
                  :disabled="!(selected.length == 1)"
                  >
                  <v-icon>mdi-file-export</v-icon>
                </v-btn>
              </template>
              <span>Export part data to CSV</span>
            </v-tooltip>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="searchTable"
                    id="searchbtn"
                    >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </template>
                <span>Search</span>
              </v-tooltip>
            </v-app-bar>
            <v-container fluid style="height: 1500px; overflow: auto;">
              <v-data-table
                v-model="selected"
                :headers="headers"
                :items="itemsWithSno4"
                item-key="maName"
                :search="search"
                hide-default-footer
                fixed-header
                height="auto"
                show-select
                elevation="8"
                checkbox-color="teal lighten-2"
                must-sort
                @item-selected="disableRePushBtn"
                @toggle-select-all="selectAll"
              >
                <template v-slot:header.maName="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false">
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="maName ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 280px">
                      <v-text-field
                        v-model="maName"
                        class="pa-4"
                        type="text"
                        label="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
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
                        >Clean</v-btn
                      >
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
          :value="'tab-5'"
          :transition="false"
          :reverse-transition="false"
        >
          <v-sheet class="overflow-y-auto" max-height="800">
            <v-app-bar class="ma-5" color="white" flat>
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn class="buttons" @click="rePush"
                  depressed
                  color="primary"
                  icon
                  :disabled="!failedStatus"
                  v-bind="attrs"
                  v-on="on">
                  <v-icon>mdi-arrow-u-up-right-bold</v-icon>
                  </v-btn>
                </template>
                <span>Re-Push to SAP</span>
              </v-tooltip>
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_table_to_csv"
                    >
                    <v-icon>mdi-export-variant</v-icon>
                  </v-btn>
                </template>
                <span>Export table to CSV</span>
              </v-tooltip>
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="export_part_data"
                    :disabled="!(selected.length == 1)"
                    >
                    <v-icon>mdi-file-export</v-icon>
                  </v-btn>
                </template>
                <span>Export part data to CSV</span>
              </v-tooltip>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    depressed
                    color="primary"
                    icon
                    class="buttons"
                    @click="searchTable"
                    id="searchbtn"
                    >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </template>
                <span>Search</span>
              </v-tooltip>
             
            </v-app-bar>
            <v-container fluid style="height: 1500px; overflow: auto;">
              <v-data-table
                display: block
                v-model="selected"
                :headers="headers1"
                :items="itemsWithSno"
                item-key="maName"
                :search="search"
                fixed-header
                height= "auto"
                show-select
                elevation="8"
                checkbox-color="teal lighten-2"
                resizable="true"
                must-sort
                :custom-sort="customSort"
                @item-selected="disableRePushBtn"
                @toggle-select-all="selectAll"
              >
              <!-- <template v-slot:item.caCompletedTime="{ item }">
                <div class="col-8 text-truncate">
                {{item.caCompletedTime}}
                </div>
                </template> -->
                <template v-slot:item.status="{ item }">
                  <v-chip :color="getColor(item.status)">
                    {{ item.status }}
                  </v-chip>
                </template>
                <template v-slot:header.maName="{ header }">
                  {{ header.text }}
                  <v-menu offset-y :close-on-content-click="false">
                    <template v-slot:activator="{ on, attrs }">
                      <v-btn icon v-bind="attrs" v-on="on">
                        <v-icon small :color="maName ? 'primary' : ''">
                          mdi-magnify-expand
                        </v-icon>
                      </v-btn>
                    </template>
                    <div style="background-color: white; width: 250px">
                      <v-text-field
                        id="colfilter"
                        full-width
                        placeholder="Enter the search term"
                        v-model="maName"
                        class="pa-4"
                        type="text"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="status = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="revision = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="title = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="maturity = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="caName = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="description = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
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
                        full-width
                        placeholder="Enter the search term"
                        :autofocus="true"
                      ></v-text-field>
                      <v-btn
                        @click="caCompletedTime = ''"
                        small
                        text
                        color="primary"
                        class="ml-2 mb-2"
                        >Clean</v-btn
                      >
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
						</div>`;
      $body.html(vueTags);
    },
    webserviceToGetAllBOMComponents: function(){
      var _this = this;
      WAFData.authenticatedRequest(widget.getValue('webserviceURLToGetAllBOMComponents'), {
        method: "GET",
        accept: "application/json",
        onComplete: function(dataResp) {
          var data = JSON.parse(dataResp);
          _this.vueapp.details = data;
          _this.vueapp.filterData = data;
        },
        onFailure: function(error) {
          console.log(error);
        }
      });
    },
    webserviceForRepush: function(){
      debugger
      var _this = this;
      // _this.vueapp.snackbar_color = "success";
      // _this.vueapp.snackbar = true;
      // _this.vueapp.snackbarMsg.push("SUCCESS!!");
      // let i = 0;
      for(let i=0; i<_this.vueapp.selected.length; i++) {
      var Ca_ID = _this.vueapp.selected[i].caID;
            var BOM_Comp_ID = _this.vueapp.selected[i].maID;
            var ma_name = _this.vueapp.selected[i].maName;
            var Connection_ID = _this.vueapp.selected[i].ConnectionID;
            var objSelected = {
                            CAID :Ca_ID,
                            BOMComponentID:BOM_Comp_ID,
                            ConnectionID:Connection_ID,
                            BOMName: ma_name
                          };

      WAFData.authenticatedRequest(widget.getValue('webserviceURLForRepush'), {
        method: "POST",
        accept: "application/json",
        crossOrigin: true,
        timeout: 7000,
        data : JSON.stringify(objSelected),
        headers: {
          'Content-Type': 'application/json'
          },
          
        onComplete: function(myJson) {
          var returnData = JSON.parse(myJson);
          // _this.vueapp.responseData = returnData;
          _this.vueapp.snackbarMsg.push(returnData);
          console.log("MYJSON -->" + myJson);
          if(_this.vueapp.snackbarMsg.response == "Success") {
            _this.vueapp.snackbar = true;
            _this.vueapp.filteredData.map( x => {
              _this.vueapp.selected.map( y =>{
                if(x.maID == y.maID) {
                  x.PLM_SAP_Integration_nls.SapFeedbackMessage = "JSON Format Validation Completed";
                }
              }
              )
            })
          }
          else {
            _this.vueapp.snackbar_color = "error";
            _this.vueapp.snackbarMsg.push(returnData);
            _this.vueapp.snackbar = true;
            _this.vueapp.filteredData.map( x => {
              _this.vueapp.selected.map( y =>{
                if(x.maID == y.maID) {
                  x.status = PLM_SAP_Integration_nls.failed;
                  x.PLM_SAP_Integration_nls.SapFeedbackMessage = "JSON Format Validation failed";
                }
              }
              )
            })
          }
        //   console.log("DATA RESPONCE FROM RE-PUSH--->" +returnData);
        //   _this.vueapp.filteredData.map( x => {
        //     _this.vueapp.selected.map( y =>{
        //       if(x.maID == y.maID) {
        //         x.SapFeedbackMessage = "JSON Format Validation Completed";
        //       }
        //     }
        //     )
        //   })
        },
        onFailure: function(error) {
          console.log(error);
          alert("ERROR OCCURED!!!");
          _this.vueapp.snackbar_color = "error";
          _this.vueapp.snackbarMsg.push("ERROR OCCURED!!!");
          console.log( _this.vueapp.snackbarMsg);
          _this.vueapp.snackbar = true;

          _this.vueapp.filteredData.map( x => {
            _this.vueapp.selected.map( y =>{
              if(x.maID == y.maID) {
                x.status = PLM_SAP_Integration_nls.failed;
              }
            }
            )
          })
        }
      });
    }
    _this.vueapp.selected = [];
    // _this.vueapp.failedSelectedCount = null;
    },
    webserviceToExportPartData: function(){
      var _this = this;
      WAFData.authenticatedRequest(widget.getValue('webserviceURLToExportPartData'), {
        method: "GET",
        accept: "application/json",
        onComplete: function(dataResp) {
          var data = JSON.parse(dataResp);
          _this.vueapp.partData = data;
        },
        onFailure: function(error) {
          console.log(error);
        }
      });
    },
    loadData: function () {
      var vueapp = new Vue({
        el: "#app",
        vuetify: new Vuetify(),
        data: {
          partData: [],
          snackbar_color: "success",
          arrObj: [],
          failedStatus: false,
          snackbar: false,
          snackbarMsg : [],
            type: 1,
            active: true,
            maName: "",
            status: "",
            revision: "",
            title: "",
            maturity: "",
            description: "",
            caCompletedTime: "",
            caName: "",
            SapFeedbackTimeStamp: "",
            SapFeedbackMessage: "",
            filterData: [],
            model: null,
            singleSelect: false,
            search: "",
            globalSearch: "",
            details:[],
            selected: [],
            headers1: PLM_SAP_Integration_nls.headers1,
            headers: PLM_SAP_Integration_nls.headers,
            time: "",
            date: "",
            failedSelectedCount: null,
            notFailedSelectedCount: 0
        },
        computed: {
          getDtaToBeSent(){
            const a = this.selected.map((obj) => [obj.maName, obj.maID, obj.caID, obj.ConnectionID]);
            this.maDetails = a;
            console.log(a);
          },
          // checkbox method to enable only failed ones
          // onlyFailed() {
            // return this.filteredData.map(x => ({
            //   ...x,
            //   isSelectable: x.status == PLM_SAP_Integration_nls.failed
            // }))
          // },
          itemsWithSno() {
            return this.filteredData.map((d, index) => ({ ...d, sno: index + 1}));
          },
          itemsWithSno1() {
            return this.waitingTable.map((d, index) => ({ ...d, sno: index + 1 }));
          },
          itemsWithSno2() {
            return this.inworkTable.map((d, index) => ({ ...d, sno: index + 1 }));
          },
          itemsWithSno3() {
            return this.successTable.map((d, index) => ({ ...d, sno: index + 1 }));
          },
          itemsWithSno4() {
            return this.failedTable.map((d, index) => ({ ...d, sno: index + 1 }));
          },
          filteredData() {
            let conditions = [];
            if (this.maName) {
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
              return this.filterData.filter(name => {
                return conditions.every(condition => {
                  return condition(name);
                });
              });
            }
            return this.filterData;
          },
          successTable() {
            return this.filteredData.filter(x => x.status == PLM_SAP_Integration_nls.success);
          },
          failedTable() {
            return this.filteredData.filter(x => x.status == PLM_SAP_Integration_nls.failed);
          },
          waitingTable() {
            return this.filteredData.filter(x => x.status == PLM_SAP_Integration_nls.waiting);
          },
          inworkTable() {
            return this.filteredData.filter(x => x.status == PLM_SAP_Integration_nls.inWork);
          }
        },
        methods: {
          selectAll(obj1) {
            if(!obj1.value){
              this.arrObj = []
            }
            if(obj1.items.length == obj1.items.filter(x => x.status == PLM_SAP_Integration_nls.failed).length){
                this.failedStatus = true
            } else {
                this.failedStatus = false
            }
        },
          disableRePushBtn(obj){
            debugger
            if(this.selected.length == this.filteredData.length){
              this.arrObj = this.selected
            }
             if(obj.value){
              this.arrObj.push(obj.item);
            }
            else{
              var index = this.arrObj.indexOf(obj.item);
              this.arrObj.splice(index, 1);
              // arrobj = arrobj.filter(item => item !== obj);
            }
            if(this.arrObj.filter(x => x.status !== PLM_SAP_Integration_nls.failed).length > 0){
              this.failedStatus = false
            } else if(this.arrObj.filter(x => x.status == PLM_SAP_Integration_nls.failed).length > 0){
              this.failedStatus = true
            } else {
              this.failedStatus = false
            }
          },
          customSort: function(items, index, isDesc) {
            items.sort((a, b) => {
                if (index[0]==PLM_SAP_Integration_nls.caCompletedTime) {
                  if (!isDesc[0]) {
                      return new Date(b[index]) - new Date(a[index]);
                  } else {
                      return new Date(a[index]) - new Date(b[index]);
                  }
                }
                else if (index[0]==PLM_SAP_Integration_nls.sno){
                  if (!isDesc[0]) {
                    return b[index] - a[index];
                } else {
                    return a[index] - b[index];
                }
                }
                else {
                  if(typeof a[index] != 'undefined'){
                    if (!isDesc[0]) {
                       return a[index].toLowerCase().localeCompare(b[index].toLowerCase());
                    }
                    else {
                        return b[index].toLowerCase().localeCompare(a[index].toLowerCase());
                    }
                  }
                }
            });
            return items;
          },
          download_csv(csv, filename) {
            var csvFile;
            var downloadLink;
        
            // CSV FILE
            csvFile = new Blob([csv], {type: "text/csv"});
        
            // Download link
            downloadLink = document.createElement("a");
        
            // File name
            downloadLink.download = filename;
        
            // We have to create a link to the file
            downloadLink.href = window.URL.createObjectURL(csvFile);
        
            // Make sure that the link is not displayed
            downloadLink.style.display = "none";
        
            // Add the link to your DOM
            document.body.appendChild(downloadLink);
        
            // Lanzamos
            downloadLink.click();
          },
          export_table_to_csv(html, filename) {
            var _this = this;
            var csv = [];
            var rows = document.querySelectorAll("table tr");
              for (var i = 0; i < rows.length; i++) {
                var row = [], cols = rows[i].querySelectorAll("td, th");
                  for (var j = 1; j < cols.length; j++){
                      row.push(cols[j].innerText);
                      csv.push(row.join(","));
                  }
              }
              // Download CSV
              _this.download_csv(csv.join("\n"), "3DX-SAP Integration");
          },
          export_part_data(){
            var _this = this;
            alert("Method to export part data as CSV.");
            myWidget.webserviceToExportPartData();
            _this.download_csv(this.partData.join("\n"), "Sub contract part");
          },
          getColor(status) {
            if (status === PLM_SAP_Integration_nls.success) return "green lighten-1";
            else if (status === PLM_SAP_Integration_nls.failed) return "red lighten-1";
            else if (status === PLM_SAP_Integration_nls.inWork) return "blue lighten-1";
            else if (status === PLM_SAP_Integration_nls.waiting) return "yellow lighten-1";
          },
          filtermaName(item) {
            return item.maName.toLowerCase().includes(this.maName.toLowerCase());
          },
          filterStatus(item) {
            return item.status.toLowerCase().includes(this.status.toLowerCase());
          },
          filterRevision(item) {
            return item.revision.toLowerCase().includes(this.revision.toLowerCase());
          },
          filterTitle(item) {
            return item.title.toLowerCase().includes(this.title.toLowerCase());
          },
          filterMaturity(item) {
            return item.maturity.toLowerCase().includes(this.maturity.toLowerCase());
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
            return item.caName.toLowerCase().includes(this.caName.toLowerCase());
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
          rePush() {
            this.filteredData.map( x => {
              this.selected.map( y =>{
                if(x.maID == y.maID) {
                  x.status = PLM_SAP_Integration_nls.inWork;
                }
              }
              )
            }
            )
            myWidget.webserviceForRepush();
            // this.filteredData.map(x => {
            //   x.status = "In Work";
              // this.filteredData.push(x);
              // this.failedTable.map(x);
              // const index = this.employees.indexOf(this.se[i]);
              // this.failedTable.splice(x, 1);
            // });
            // for (var i = 0; i < this.selected.length; i++) {
            //   const index = this.failedTable.indexOf(this.selected[i]);
            //   this.failedTable.splice(index, 1);
            // }
          },
          clear() {
            this.filterData = this.details;
          },
          searchTable() {
            if (this.globalSearch === "" || this.globalSearch === null) {
              this.filterData = this.details;
            } else {
              return this.filterData = this.details.filter(data => {
                return (
                  data.maName
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
                  data.SapFeedbackTimeStamp.toLowerCase().includes(
                    this.globalSearch.toLowerCase()
                  ) ||
                  data.SapFeedbackMessage.toLowerCase().includes(
                    this.globalSearch.toLowerCase()
                  )
                );
              });
            }
          }
        },
        mounted() {
          // this.filterData = this.details;
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
        }
      });
      myWidget.vueapp = vueapp;
    },
    onRefresh: function () {
      myWidget.onLoad();
    },
  };
  return myWidget;
});

<template>
  <div class="staff-wrap">
    <header class="staff-commandbar glass">
      <div class="staff-commandbar__brand">
        <svg class="staff-commandbar__mark" width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.4" />
          <path d="M12 2c4 3.5 4 16.5 0 20" stroke="currentColor" stroke-width="1.1" opacity=".55"/>
          <path d="M2 12c3.5-4 16.5-4 20 0" stroke="currentColor" stroke-width="1.1" opacity=".55"/>
          <circle cx="12" cy="12" r="2.4" fill="currentColor"/>
        </svg>
        <div class="staff-commandbar__name">
          <strong>员工运营台</strong>
          <span class="mono">DISPATCH / OPS</span>
        </div>
      </div>
      <div class="staff-commandbar__tabs">
        <button class="staff-tab" :class="{ active: tab === 'demands' }" @click="tab = 'demands'">
          需求信息 <span class="staff-tab__count mono">{{ demands.length }}</span>
        </button>
        <button class="staff-tab" :class="{ active: tab === 'routes' }" @click="tab = 'routes'">路线管理</button>
        <button class="staff-tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">订单管理</button>
        <button class="staff-tab" :class="{ active: tab === 'events' }" @click="tab = 'events'">物流动态</button>
        <button class="staff-tab" :class="{ active: tab === 'reviews' }" @click="tab = 'reviews'">评论管理</button>
        <button class="staff-tab" :class="{ active: tab === 'profile' }" @click="tab = 'profile'">个人中心</button>
      </div>
      <div class="staff-commandbar__meta">
        <span class="mono">GIS 跨区域配送</span>
        <button class="staff-logout" @click="logout">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2m4-6h4m0 0l-3-3m3 3l-3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
          退出
        </button>
      </div>
    </header>

    <!-- 需求信息：查看 + 报价接单 -->
    <section v-if="tab === 'demands'">
      <div class="staff-kpis" v-if="demands.length">
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">DEMANDS</span></div>
          <div class="staff-kpi__num mono">{{ demands.length }}</div>
          <div class="staff-kpi__label">待对接需求</div>
        </div>
        <div class="staff-kpi glass accent">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">MATCH</span></div>
          <div class="staff-kpi__num mono">{{ inAreaCount }}</div>
          <div class="staff-kpi__label">配送范围内</div>
        </div>
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">RANGE</span></div>
          <div class="staff-kpi__num mono" style="font-size:20px">{{ deliveryArea || '未设置' }}</div>
          <div class="staff-kpi__label">我的配送范围</div>
        </div>
      </div>
      <div class="staff-panel glass">
        <div class="staff-panel__head">
          <span class="staff-panel__title">需求信息</span>
          <span class="staff-panel__hint mono">{{ filteredDemands.length }} / {{ demands.length }} 条</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
        <div class="staff-filter">
          <el-select v-model="fRegion" placeholder="区域" clearable class="staff-filter__sel">
            <el-option v-for="r in regions" :key="r" :label="r" :value="r" />
          </el-select>
          <el-select v-model="fStatus" placeholder="状态" clearable class="staff-filter__sel">
            <el-option label="待报价" value="PENDING" />
            <el-option label="已报价" value="QUOTED" />
            <el-option label="已接单" value="ACCEPTED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-input-number v-model="fWeight" :min="0" placeholder="重量≥(g)" :controls="false" class="staff-filter__num" />
          <el-switch v-model="fInArea" active-text="仅范围内" class="staff-filter__switch" />
          <button class="staff-filter__reset" @click="resetFilter">清除</button>
        </div>
        <el-table :data="filteredDemands" class="staff-table" row-key="id" :expand-row-keys="expandedDemands">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="staff-demand-detail">
                <div class="staff-demand-detail__row"><span>物品</span><b>{{ row.title }} · {{ row.weightG / 1000 }}kg{{ row.fragile ? ' · 易碎' : '' }}</b></div>
                <div class="staff-demand-detail__row"><span>体积</span><b class="mono">{{ (row.volumeCm3 / 1e6).toFixed(2) }} m³</b></div>
                <div class="staff-demand-detail__row"><span>寄件</span><b>{{ row.originRegion }} · {{ row.originAddr }}</b></div>
                <div class="staff-demand-detail__row"><span>收件</span><b>{{ row.targetRegion }} · {{ row.targetAddr }}</b></div>
                <div class="staff-demand-detail__row"><span>距离</span><b class="mono">≈ {{ demandDistance(row) }} km</b></div>
                <div class="staff-demand-detail__row"><span>配送范围匹配</span><b :class="row.originRegion === deliveryArea || row.targetRegion === deliveryArea ? 'ok' : 'warn'">{{ inDeliveryArea(row) ? '在范围内' : '范围外' }}</b></div>
                <div class="staff-demand-detail__row">
                  <span>接取操作</span>
                  <b class="staff-demand-detail__actions">
                    <el-button size="small" type="primary" plain @click="useDemandAsRoute(row)">用作路线搜索</el-button>
                    <el-button
                      size="small"
                      type="primary"
                      :disabled="row.status === 'CLOSED' || row.status === 'ACCEPTED'"
                      @click="openQuote(row)"
                    >报价接单</el-button>
                    <span class="staff-demand-detail__hint mono">带出路线 / 直接报价</span>
                  </b>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="ID" width="64" class-name="mono"  />
          <el-table-column prop="title" label="标题" min-width="140"  />
          <el-table-column prop="originRegion" label="出发" width="80"  />
          <el-table-column prop="targetRegion" label="到达" width="80"  />
          <el-table-column label="距离" width="84" class-name="mono">
            <template #default="{ row }">≈ {{ demandDistance(row) }}km</template>
          </el-table-column>
          <el-table-column prop="weightG" label="重量(g)" width="90" class-name="mono"  />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="toggleDemandExpand(row)">{{ expandedDemands.includes(row.id) ? '收起' : '详情' }}</el-button>
              <el-button size="small" type="primary" :disabled="row.status === 'CLOSED' || row.status === 'ACCEPTED'" @click="openQuote(row)">报价接单</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!demands.length" description="暂无待接单需求" />
      </div>
    </section>

    <!-- 路线管理 -->
    <section v-else-if="tab === 'routes'">
      <div class="staff-ordergrid">
        <div class="staff-mapglass glass">
          <div class="staff-maphead">
            <span class="staff-maphead__title">路线 / 轨迹</span>
            <span class="staff-sse" v-if="activeOrderId">
              <span class="status-dot" :class="sseConnected ? 'status-dot--live' : 'status-dot--stale'"></span>
              <span class="mono">{{ sseConnected ? 'LIVE' : 'RECONNECTING' }}</span>
            </span>
          </div>
          <GisMap
            v-if="routeLines.length > 0 || trackPoints.length > 0"
            :lines="routeLines"
            :points="trackPoints"
            class="staff-mapcanvas"
          />
          <div v-else class="staff-mapempty">选择订单并「规划路线」以加载轨迹</div>
        </div>
        <div class="staff-orderslist">
          <div class="staff-panel__head">
            <span class="staff-panel__title">路线规划 / 搜索</span>
            <el-button size="small" @click="load">刷新</el-button>
          </div>
          <div class="staff-routesearch">
            <div class="staff-routesearch__head">
              <span class="staff-panel__title">路线搜索</span>
              <span class="staff-panel__hint mono">途经 / 规避 → 距离 / ETA</span>
            </div>
            <div class="staff-routesearch__row staff-routesearch__row--split">
              <el-select v-model="searchStartProv" placeholder="起点省" clearable class="staff-routesearch__in staff-routesearch__in--prov">
                <el-option v-for="p in provinceOptions" :key="p" :label="p" :value="p" />
              </el-select>
              <el-select v-model="searchStartCity" placeholder="起点市" filterable clearable class="staff-routesearch__in" allow-create>
                <el-option v-for="c in startCityOptions" :key="c" :label="c" :value="c" />
              </el-select>
              <el-select v-model="searchEndProv" placeholder="终点省" clearable class="staff-routesearch__in staff-routesearch__in--prov">
                <el-option v-for="p in provinceOptions" :key="p" :label="p" :value="p" />
              </el-select>
              <el-select v-model="searchEndCity" placeholder="终点市" filterable clearable class="staff-routesearch__in" allow-create>
                <el-option v-for="c in endCityOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <el-input v-model="searchVia" placeholder="途经地点（逗号分隔，可空）" clearable class="staff-routesearch__in" />
            <el-input v-model="searchAvoid" placeholder="规避路段（逗号分隔，可空）" clearable class="staff-routesearch__in" />
            <el-button type="primary" size="small" class="staff-routesearch__go" @click="onSearchRoute">搜索路线</el-button>
            <div v-if="searchResult" class="staff-routesearch__result mono">
              <div class="staff-routesearch__main">
                <span>距离 {{ searchResult.distanceKm }} km</span>
                <span>≈ {{ searchResult.etaMinutes }} min</span>
                <span>途经 {{ searchResult.via.length ? searchResult.via.join('、') : '—' }}</span>
                <el-button size="small" text @click="searchDetailOpen = !searchDetailOpen">{{ searchDetailOpen ? '收起详情' : '查看详情' }}</el-button>
              </div>
              <div v-if="searchDetailOpen" class="staff-routesearch__detail">
                <div class="staff-routesearch__drow"><span>起点</span><b class="mono">{{ searchStart || '未选择' }} · {{ coordText(searchStartCoord[0], searchStartCoord[1]) }}</b></div>
                <div class="staff-routesearch__drow"><span>终点</span><b class="mono">{{ searchEnd || '未选择' }} · {{ coordText(searchEndCoord[0], searchEndCoord[1]) }}</b></div>
                <div class="staff-routesearch__drow" v-if="searchResult.via.length"><span>途经节点</span><b>{{ searchResult.via.join(' → ') }}</b></div>
                <div class="staff-routesearch__drow" v-if="searchAvoid"><span>规避路段</span><b class="warn">{{ searchAvoid }}</b></div>
                <div class="staff-routesearch__drow"><span>预计到达</span><b class="mono">{{ etaClock }}</b></div>
                <div class="staff-routesearch__drow"><span>路线引擎</span><b>{{ orders.length ? 'GIS 已上图' : '本地估算' }}</b></div>
              </div>
            </div>
          </div>
          <el-table :data="orders" class="staff-table">
            <el-table-column prop="id" label="订单" width="80" class-name="mono"  />
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="起点 / 终点" min-width="170">
              <template #default="{ row }">
                <span class="staff-ep-region">{{ orderRegionText(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="280">
              <template #default="{ row }">
                <div class="staff-actrow">
                  <el-button size="small" type="primary" @click="onPlanRoute(row)">规划路线</el-button>
                  <el-button size="small" @click="onRecommendRoute(row)">推荐路线</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!orders.length" description="暂无订单" />

          <div v-if="recommendations.length" class="staff-recs">
            <div class="staff-recs__head">
              <span class="staff-panel__title">路线推荐 · 订单 #{{ recommendOrderId }}</span>
            </div>
            <div class="staff-recs__grid">
              <div
                v-for="r in recommendations"
                :key="r.strategy"
                class="staff-rec"
                :class="{ 'staff-rec--best': r.strategy === bestStrategy }"
              >
                <div class="staff-rec__top">
                  <b>{{ strategyLabel(r.strategy) }}</b>
                  <el-tag v-if="r.strategy === bestStrategy" size="small" type="success">推荐</el-tag>
                </div>
                <div class="staff-rec__metrics mono">
                  <span>{{ r.distanceM != null ? (r.distanceM / 1000).toFixed(1) + ' km' : '—' }}</span>
                  <span>{{ r.etaS != null ? Math.round(r.etaS / 60) + ' min' : '—' }}</span>
                  <span>{{ r.cost != null ? '¥' + (r.cost / 100).toFixed(0) : '—' }}</span>
                </div>
                <div class="staff-rec__hint">距离 / 时效 / 成本</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 订单管理 -->
    <section v-else-if="tab === 'orders'">
      <div class="staff-kpis" v-if="orders.length">
        <div class="staff-kpi glass accent">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">ACTIVE</span></div>
          <div class="staff-kpi__num mono">{{ activeOrderCount }}</div>
          <div class="staff-kpi__label">进行中订单</div>
        </div>
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">GROSS</span></div>
          <div class="staff-kpi__num mono">¥{{ grossFen }}</div>
          <div class="staff-kpi__label">在途/待派总额</div>
        </div>
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">DONE</span></div>
          <div class="staff-kpi__num mono">{{ doneOrderCount }}</div>
          <div class="staff-kpi__label">已完成签收</div>
        </div>
      </div>
      <div class="staff-panel glass">
        <div class="staff-panel__head">
          <span class="staff-panel__title">我的订单</span>
          <span class="staff-panel__hint mono">{{ orders.length }} 条</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
        <el-table :data="orders" class="staff-table">
          <el-table-column prop="id" label="订单" width="80" class-name="mono"  />
          <el-table-column prop="status" label="状态" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额(分)" width="100" class-name="mono"  />
          <el-table-column label="起点 / 终点" min-width="170">
            <template #default="{ row }">
              <span class="staff-ep-region">{{ orderRegionText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="280">
            <template #default="{ row }">
              <div class="staff-actrow">
                <el-button size="small" type="primary" @click="onPlanRoute(row)">规划路线</el-button>
                <el-button size="small" :disabled="!canAdvance(row.status)" @click="advanceOrder(row)">推进配送</el-button>
                <el-button size="small" type="warning" @click="onPublishEvent(row)">发布动态</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!orders.length" description="暂无订单" />
      </div>
    </section>

    <!-- 物流信息发布 -->
    <section v-else>
      <div class="staff-kpis" v-if="orders.length">
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">EVENTS</span></div>
          <div class="staff-kpi__num mono">{{ evHistory.length }}</div>
          <div class="staff-kpi__label">当前订单动态数</div>
        </div>
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">LIVE</span></div>
          <div class="staff-kpi__num mono" style="font-size:20px">{{ activeOrderId ? '#' + activeOrderId : '—' }}</div>
          <div class="staff-kpi__label">正在跟踪的订单</div>
        </div>
      </div>
      <div class="staff-ordergrid">
        <div class="staff-mapglass glass">
          <div class="staff-maphead"><span class="staff-maphead__title">轨迹回放</span></div>
          <GisMap
            v-if="trackPoints.length > 0"
            :lines="routeLines"
            :points="trackPoints"
            class="staff-mapcanvas"
          />
          <div v-else class="staff-mapempty">发布动态后此处显示轨迹</div>
        </div>
        <div class="staff-orderslist glass">
          <div class="staff-panel__head"><span class="staff-panel__title">发布物流动态</span><span class="staff-panel__hint mono">节点同步用户端</span></div>
          <div class="staff-eventform">
            <el-select v-model="evOrder" placeholder="选择订单" class="staff-eventform__sel" @change="onEvOrderChange">
              <el-option v-for="o in orders" :key="o.id" :label="'#' + o.id + ' ' + o.status + (isTerminalStatus(o.status) ? '（终态）' : '')" :value="o.id" />
            </el-select>
            <el-select v-model="evType" class="staff-eventform__sel" :disabled="isTerminalStatus(currentEvOrderStatus)" :placeholder="isTerminalStatus(currentEvOrderStatus) ? '终态订单不可发布动态' : ''">
              <el-option label="揽件" value="PICKED" />
              <el-option label="在途" value="IN_TRANSIT" />
              <el-option label="到达网点" value="ARRIVED_DELIVERY" />
              <el-option label="签收" value="DELIVERED" />
            </el-select>
            <el-input v-model="evCoords" placeholder="经纬度：lon,lat（可留空）" class="staff-eventform__coords" :disabled="isTerminalStatus(currentEvOrderStatus)" />
            <el-button type="primary" size="small" :loading="evSending" :disabled="isTerminalStatus(currentEvOrderStatus)" @click="onPublishEvent2">发布</el-button>
          </div>
          <el-table :data="evHistory" class="staff-table">
            <el-table-column label="类型" width="140">
              <template #default="{ row }">{{ eventLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="occurredAt" label="时间" class-name="mono"  />
          </el-table>
        </div>
      </div>
    </section>

    <!-- 评论管理：查看 + 回复 -->
    <section v-if="tab === 'reviews'">
      <div class="staff-kpis" v-if="reviews.length">
        <div class="staff-kpi glass accent">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">RATING</span></div>
          <div class="staff-kpi__num mono">{{ avgReviewRating }}</div>
          <div class="staff-kpi__label">平均评分</div>
        </div>
        <div class="staff-kpi glass">
          <div class="staff-kpi__top"><span class="staff-kpi__tag">UNREPLIED</span></div>
          <div class="staff-kpi__num mono">{{ unrepliedCount }}</div>
          <div class="staff-kpi__label">待回复评价</div>
        </div>
      </div>
      <div class="staff-panel glass">
        <div class="staff-panel__head">
          <span class="staff-panel__title">我的服务评价</span>
          <span class="staff-panel__hint mono">{{ reviews.length }} 条</span>
          <el-button size="small" @click="loadReviews">刷新</el-button>
        </div>
        <el-table :data="reviews" class="staff-table">
          <el-table-column prop="id" label="ID" width="70" class-name="mono" />
          <el-table-column prop="orderId" label="订单" width="80" class-name="mono" />
          <el-table-column label="评分" width="90">
            <template #default="{ row }">
              <span class="staff-star mono">{{ '★'.repeat(row.rating) }}{{ '☆'.repeat(5 - row.rating) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="180" />
          <el-table-column label="回复" min-width="200">
            <template #default="{ row }">
              <div v-if="row.staffReply" class="staff-reply">
                <span class="staff-reply__tag mono">已回复</span>
                <span>{{ row.staffReply }}</span>
              </div>
              <div v-else class="staff-reply staff-reply--empty">未回复</div>
              <el-input
                v-if="!row.staffReply"
                v-model="replyDrafts[row.id]"
                placeholder="输入回复内容…"
                class="staff-reply-input"
              />
              <el-button
                v-if="!row.staffReply"
                size="small"
                type="primary"
                :disabled="!replyDrafts[row.id]"
                @click="onReply(row)"
              >发送回复</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reviews.length" description="暂无用户评价" />
      </div>
    </section>

    <!-- 个人中心 -->
    <section v-if="tab === 'profile'">
      <div class="staff-panel glass">
        <div class="staff-panel__head">
          <span class="staff-panel__title">个人中心</span>
          <el-button size="small" @click="loadProfile">刷新</el-button>
        </div>
        <div v-if="!profile" class="staff-mapempty">加载中…</div>
        <div v-else class="staff-profile">
          <div class="staff-profile__ident">
            <div class="staff-profile__avatar mono">{{ (profile.name || '员').slice(0, 1) }}</div>
            <div class="staff-profile__ident-meta">
              <div class="staff-profile__name">{{ profile.name || '员工' }}</div>
              <div class="staff-profile__sub mono">执照 {{ profile.licenseNo }}</div>
              <div class="staff-profile__sub mono">所属站点：{{ profile.siteName || '—' }}</div>
            </div>
          </div>
          <div class="staff-profile__stats staff-bento">
            <div class="staff-stat glass staff-stat--hero">
              <div class="staff-stat__tag mono">ORDERS</div>
              <span class="staff-stat__num mono">{{ profile.totalOrders }}</span>
              <span class="staff-stat__label">累计接单</span>
            </div>
            <div class="staff-stat glass">
              <div class="staff-stat__tag mono">DONE</div>
              <span class="staff-stat__num mono">{{ profile.delivered }}</span>
              <span class="staff-stat__label">已完成</span>
            </div>
            <div class="staff-stat glass">
              <div class="staff-stat__tag mono">IN-TRANSIT</div>
              <span class="staff-stat__num mono">{{ profile.inTransit }}</span>
              <span class="staff-stat__label">配送中</span>
            </div>
            <div class="staff-stat glass staff-stat--accent">
              <div class="staff-stat__tag mono">RATING</div>
              <span class="staff-stat__num mono">{{ profile.avgRating ? profile.avgRating.toFixed(1) : '—' }}</span>
              <span class="staff-stat__label">平均评分</span>
            </div>
          </div>

          <!-- 编辑资料 -->
          <div class="staff-profile__edit">
            <div class="staff-profile__edit-title">编辑资料</div>
            <div class="staff-profile__edit-row">
              <el-input v-model="editName" placeholder="姓名" class="staff-profile__edit-in" />
              <el-input v-model="editLicense" placeholder="执业资质编号" class="staff-profile__edit-in" />
              <el-button size="small" type="primary" :loading="profileSaving" @click="saveProfile">保存</el-button>
            </div>
          </div>

          <!-- 修改密码 -->
          <div class="staff-profile__edit">
            <div class="staff-profile__edit-title">修改密码</div>
            <div class="staff-profile__edit-row">
              <el-input v-model="pwdOld" type="password" show-password placeholder="原密码" class="staff-profile__edit-in" />
              <el-input v-model="pwdNew" type="password" show-password placeholder="新密码（≥6位）" class="staff-profile__edit-in" />
              <el-input v-model="pwdConfirm" type="password" show-password placeholder="确认新密码" class="staff-profile__edit-in" />
              <el-button size="small" type="primary" :loading="pwdSaving" @click="changePassword">确认修改</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 报价面板 -->
    <el-dialog v-model="quoteOpen" title="报价接单" width="440px" :close-on-click-modal="false" class="staff-quote-dialog">
      <div v-if="quoteRow" class="staff-quote">
        <div class="staff-quote__meta">
          <div class="staff-quote__row"><span>需求</span><b>#{{ quoteRow.id }} {{ quoteRow.title }}</b></div>
          <div class="staff-quote__row"><span>路线</span><b>{{ quoteRow.originRegion }} → {{ quoteRow.targetRegion }}</b></div>
          <div class="staff-quote__row"><span>距离</span><b class="mono">≈ {{ quoteDistance }} km</b></div>
          <div class="staff-quote__row"><span>物品</span><b class="mono">{{ quoteRow.weightG / 1000 }} kg{{ quoteRow.fragile ? ' · 易碎' : '' }}</b></div>
          <div class="staff-quote__row"><span>时效</span>
            <el-radio-group v-model="quoteUrgent" size="small">
              <el-radio :label="false">标准</el-radio>
              <el-radio :label="true">加急</el-radio>
            </el-radio-group>
          </div>
          <div class="staff-quote__row"><span>核算</span><b class="mono" :class="{ 'quote-hot': quoteUrgent }">基础 {{ basePrice }} + 距离 {{ distPrice }} + 重量 {{ weightPrice }}{{ quoteRow.fragile ? ' + 易碎 300' : '' }}{{ quoteUrgent ? ' + 加急 20%' : '' }}</b></div>
        </div>
        <el-input-number
          v-model="quotePrice"
          :min="0"
          :step="100"
          :precision="0"
          controls-position="right"
          class="staff-quote__input"
        />
        <div class="staff-quote__unit mono">单位：分（¥{{ (quotePrice / 100).toFixed(2) }}）</div>
        <div class="staff-quote__suggest">
          <span>建议区间</span>
          <b class="mono">{{ suggestLow }} – {{ suggestHigh }} 分</b>
        </div>
      </div>
      <template #footer>
        <el-button @click="quoteOpen = false">取消</el-button>
        <el-button type="primary" :loading="quoteSending" @click="submitQuote">提交报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { REGION_CENTER, REGION_ALIAS } from '@/lib/regionCenter'
import chinaRegions from '@/data/chinaRegions.json'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { staffApi } from '@/api/staff'
import type { StaffProfileView } from '@/api/staff'
import { useTrackStream } from '@/composables/useTrackStream'
import GisMap from '@/components/gis/GisMap.vue'
import type { Demand, LogisticsEvent, Order, OrderStatus, RouteTask, Review } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'demands' | 'routes' | 'orders' | 'events' | 'reviews' | 'profile'>('demands')
const demands = ref<Demand[]>([])
const fRegion = ref('')
const fStatus = ref('')
const fWeight = ref<number | undefined>(undefined)
const fInArea = ref(false)
const expandedDemands = ref<number[]>([])
const deliveryArea = ref('')
function inDeliveryArea(row: Demand): boolean {
  if (!deliveryArea.value) return true
  return row.originRegion === deliveryArea.value || row.targetRegion === deliveryArea.value
}
function demandDistance(row: Demand): number {
  return haversineKm(centerOf(row.originRegion), centerOf(row.targetRegion))
}
const inAreaCount = computed(() => demands.value.filter((d) => inDeliveryArea(d)).length)
const activeOrderCount = computed(() => orders.value.filter((o) => !['DELIVERED', 'CANCELLED', 'REFUNDED'].includes(o.status)).length)
const doneOrderCount = computed(() => orders.value.filter((o) => o.status === 'DELIVERED').length)
const grossFen = computed(() => {
  const act = orders.value.filter((o) => !['DELIVERED', 'CANCELLED', 'REFUNDED'].includes(o.status))
  return (act.reduce((s, o) => s + (o.amount || 0), 0) / 100).toFixed(2)
})
const unrepliedCount = computed(() => reviews.value.filter((r) => !r.staffReply).length)
const avgReviewRating = computed(() => {
  if (!reviews.value.length) return '—'
  const sum = reviews.value.reduce((s, r) => s + r.rating, 0)
  return (sum / reviews.value.length).toFixed(1)
})
function toggleDemandExpand(row: Demand) {
  const i = expandedDemands.value.indexOf(row.id)
  if (i >= 0) expandedDemands.value = expandedDemands.value.filter((x) => x !== row.id)
  else expandedDemands.value = [...expandedDemands.value, row.id]
}
const regions = computed(() => Array.from(new Set(demands.value.flatMap((d) => [d.originRegion, d.targetRegion]))))
const filteredDemands = computed(() =>
  demands.value.filter((d) => {
    if (fRegion.value && d.originRegion !== fRegion.value && d.targetRegion !== fRegion.value) return false
    if (fStatus.value && d.status !== fStatus.value) return false
    if (fWeight.value !== undefined && d.weightG < fWeight.value) return false
    if (fInArea.value && !inDeliveryArea(d)) return false
    return true
  })
)
function resetFilter() {
  fRegion.value = ''
  fStatus.value = ''
  fWeight.value = undefined
  fInArea.value = false
}
const orders = ref<Order[]>([])
const currentTask = ref<RouteTask | null>(null)
const trackPoints = ref<{ lon: number; lat: number; label?: string; current?: boolean }[]>([])
const activeOrderId = ref<number | null>(null)
const evOrder = ref<number | null>(null)
const evType = ref<LogisticsEvent['type']>('IN_TRANSIT')
const evCoords = ref('')
const evSending = ref(false)
const evHistory = ref<LogisticsEvent[]>([])
const reviews = ref<Review[]>([])
const replyDrafts = ref<Record<number, string>>({})
const profile = ref<StaffProfileView | null>(null)

const { connected: sseConnected, open: openSse, close: closeSse } = useTrackStream({
  orderId: activeOrderId,
  role: 'staff',
  onSnapshot: (snap) => {
    trackPoints.value = loadPointsFromEvents(snap)
  },
  onEvent: () => {},
})
watch(tab, (t) => {
  if (t === 'demands' && !deliveryArea.value) loadProfile()
})

watch(tab, (t) => {
  if (t === 'profile' && !profile.value) loadProfile()
})

watch(activeOrderId, (newId) => {
  if (newId) openSse()
  else closeSse()
})

const routeLines = computed(() => {
  const geo = currentTask.value?.geometry
  if (!geo || geo.type !== 'LineString') return []
  return [{ type: 'LineString' as const, coordinates: geo.coordinates as [number, number][] }]
})

function parsePoint(p: unknown) {
  if (!p) return null
  if (typeof p === 'string') {
    const m = p.match(/\(([-\d.]+)\s+([-=\d.]+)\)/)
    if (!m) return null
    return { lon: Number(m[1]), lat: Number(m[2]) }
  }
  const g = p as { type?: string; coordinates?: number[] }
  if (g.type === 'Point' && Array.isArray(g.coordinates) && g.coordinates.length >= 2) {
    return { lon: Number(g.coordinates[0]), lat: Number(g.coordinates[1]) }
  }
  return null
}

async function loadTrackPoints(orderId: number) {
  try {
    const evs = await staffApi.orderEvents(orderId)
    const pts: { lon: number; lat: number; label?: string; current?: boolean }[] = []
    evs.forEach((e, i) => {
      const p = parsePoint(e.point)
      if (p) pts.push({ ...p, label: e.type, current: i === evs.length - 1 })
    })
    return pts
  } catch {
    return []
  }
}

function loadPointsFromEvents(evs: LogisticsEvent[]) {
  const pts: { lon: number; lat: number; label?: string; current?: boolean }[] = []
  evs.forEach((e, i) => {
    const p = parsePoint(e.point)
    if (p) pts.push({ ...p, label: e.type, current: i === evs.length - 1 })
  })
  return pts
}

const REGION_CITY: Record<string, string> = {
  'BJ': '北京', 'SH': '上海', 'TJ': '天津', 'CQ': '重庆',
  '江苏': '南京', '浙江': '杭州', '安徽': '合肥', '山东': '济南',
  '河北': '石家庄', '河南': '郑州', '福建': '福州', '湖北': '武汉',
  '湖南': '长沙', '广东': '广州', '四川': '成都', '陕西': '西安',
}
function regionCity(region: string): string {
  const t = region.trim()
  return t ? t + ' ' + REGION_CITY[t] : ''
}
const orderRegions = ref<Record<number, { originRegion: string; targetRegion: string }>>({})
function orderRegionText(o: Order): string {
  const r = orderRegions.value[o.id]
  if (r && (r.originRegion || r.targetRegion)) return regionCity(r.originRegion) + ' → ' + regionCity(r.targetRegion)
  if (o.fromLon != null && o.fromLat != null && o.toLon != null && o.toLat != null) return coordText(o.fromLon, o.fromLat) + ' → ' + coordText(o.toLon, o.toLat)
  return '—'
}
async function loadOrderRegions() {
  await Promise.all(
    orders.value.map(async (o) => {
      if (orderRegions.value[o.id]) return
      const r = await staffApi.orderRegions(o.id).catch(() => null)
      if (r && (r.originRegion || r.targetRegion)) {
        orderRegions.value = { ...orderRegions.value, [o.id]: { originRegion: r.originRegion || '', targetRegion: r.targetRegion || '' } }
      }
    })
  )
}
async function load() {
  try {
    demands.value = (await staffApi.openDemands()).content || []
    reviews.value = await staffApi.myReviews()
    orders.value = (await staffApi.myOrders()).content || []
    if (orders.value.length) await loadOrderRegions()
    if (evOrder.value != null) {
      evHistory.value = await staffApi.orderEvents(evOrder.value)
    }
  } catch (e) {
    console.error('load staff data failed', e)
  }
}

const quoteOpen = ref(false)
const quoteRow = ref<Demand | null>(null)
const quotePrice = ref(0)
const quoteSending = ref(false)

// 拼音 -> 城市（供搜索框输入 pinyin 也能识别）
const PINYIN_ALIAS: Record<string, string> = {
  'beijing': '北京', 'shanghai': '上海', 'tianjin': '天津', 'jinan': '济南',
  'nanjing': '南京', 'hangzhou': '杭州', 'qingdao': '青岛', 'zhengzhou': '郑州',
  'xuzhou': '徐州', 'hefei': '合肥', 'fuzhou': '福州', 'ningbo': '宁波',
  'guangzhou': '广州', 'shenzhen': '深圳', 'chengdu': '成都', 'wuhan': '武汉',
  'changsha': '长沙', 'xian': '西安', 'zhongqing': '重庆', 'shenyang': '沈阳', 'dalian': '大连',
  'jiangsu': '江苏', 'zhejiang': '浙江', 'guangdong': '广东', 'anhui': '安徽',
  'henan': '河南', 'fujian': '福建', 'hebei': '河北', 'hubei': '湖北',
  'hunan': '湖南', 'shaanxi': '陕西', 'sichuan': '四川', 'liao': '辽宁',
}
function centerOf(region?: string | null): [number, number] {
  if (!region) return REGION_CENTER['北京']
  const key = region.trim()
  // 直接命中 / 单字别名 / 拼音别名 / 模糊包含
  if (REGION_CENTER[key]) return REGION_CENTER[key]
  const alias = REGION_ALIAS[key]
  if (alias && REGION_CENTER[alias]) return REGION_CENTER[alias]
  const lower = key.toLowerCase()
  if (PINYIN_ALIAS[lower] && REGION_CENTER[PINYIN_ALIAS[lower]]) return REGION_CENTER[PINYIN_ALIAS[lower]]
  for (const name of Object.keys(REGION_CENTER)) {
    if (key.includes(name) || name.includes(key)) return REGION_CENTER[name]
  }
  return REGION_CENTER['北京']
}
function haversineKm(a: [number, number], b: [number, number]): number {
  const R = 6371
  const dLat = ((b[1] - a[1]) * Math.PI) / 180
  const dLon = ((b[0] - a[0]) * Math.PI) / 180
  const la1 = (a[1] * Math.PI) / 180
  const la2 = (b[1] * Math.PI) / 180
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(la1) * Math.cos(la2) * Math.sin(dLon / 2) ** 2
  return Math.round(2 * R * Math.asin(Math.sqrt(h)))
}
const quoteDistance = computed(() => {
  if (!quoteRow.value) return 0
  return haversineKm(centerOf(quoteRow.value.originRegion), centerOf(quoteRow.value.targetRegion))
})
// 建议报价：基础运费 + 距离×单位 + 重量加价 + 易碎加价
const quoteUrgent = ref(false)
const basePrice = 500
const distPrice = computed(() => Math.round(quoteDistance.value * 1.2))
const weightPrice = computed(() => Math.round((quoteRow.value?.weightG ?? 0) / 1000 * 40))
const suggestLow = computed(() => {
  if (!quoteRow.value) return 0
  const w = quoteRow.value.weightG / 1000
  let low = basePrice + distPrice.value + w * 40 + (quoteRow.value.fragile ? 300 : 0)
  if (quoteUrgent.value) low = Math.round(low * 1.2)
  return Math.max(0, Math.round(low))
})
const suggestHigh = computed(() => {
  if (!quoteRow.value) return 0
  return Math.round(suggestLow.value * 1.4)
})

function openQuote(row: Demand) {
  quoteRow.value = row
  quoteUrgent.value = false
  quotePrice.value = suggestLow.value
  quoteOpen.value = true
}
// 切换时效时同步刷新建议价
watch(quoteUrgent, () => {
  if (quoteRow.value) quotePrice.value = suggestLow.value
})

// 省 -> 市 级联（复用用户端同一份 chinaRegions.json，保证两端口径一致）
interface RegionDef { name: string; cities: { name: string }[] }
const regionData = chinaRegions as unknown as { provinces: RegionDef[] }
const provinceOptions = regionData.provinces.map((p) => p.name)
function citiesOf(prov: string): string[] {
  const p = regionData.provinces.find((x) => x.name === prov)
  return p ? p.cities.map((c) => c.name) : []
}
const searchStartProv = ref('')
const searchStartCity = ref('')
const searchEndProv = ref('')
const searchEndCity = ref('')
const startCityOptions = computed(() => citiesOf(searchStartProv.value))
const endCityOptions = computed(() => citiesOf(searchEndProv.value))
// 兼容旧字段：完整起终点名（省+市 或 单城市）
const searchStart = computed(() => [searchStartProv.value, searchStartCity.value].filter(Boolean).join(' '))
const searchEnd = computed(() => [searchEndProv.value, searchEndCity.value].filter(Boolean).join(' '))
const searchVia = ref('')
const searchAvoid = ref('')
const searchResult = ref<{ distanceKm: number; etaMinutes: number; via: string[] } | null>(null)
const searchDetailOpen = ref(false)
const searchStartCoord = ref<[number, number]>([116.4074, 39.9042])
const searchEndCoord = ref<[number, number]>([121.4737, 31.2304])
const coordText = (lon: number, lat: number) => lon.toFixed(3) + ', ' + lat.toFixed(3)
const etaClock = computed(() => {
  if (!searchResult.value) return '—'
  const d = new Date(Date.now() + searchResult.value.etaMinutes * 60000)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

const recommendations = ref<RouteTask[]>([])
const recommendOrderId = ref<number | null>(null)
const bestStrategy = computed(() => {
  if (!recommendations.value.length) return null
  const sorted = [...recommendations.value].sort((a, b) => (a.distanceM ?? 1e9) - (b.distanceM ?? 1e9))
  return sorted[0].strategy
})

function strategyLabel(s: RouteTask['strategy']) {
  return s === 'SHORTEST' ? '最短距离' : s === 'FASTEST' ? '最快时效' : '最低成本'
}

async function onRecommendRoute(row: Order) {
  recommendOrderId.value = row.id
  recommendations.value = []
  const ep = await resolveEndpoints(row)
  if (!ep) {
    ElMessage.warning('订单 #' + row.id + ' 缺少起终点坐标，请在「路线搜索」选择起终点')
    tab.value = 'routes'
    return
  }
  try {
    const results: RouteTask[] = []
    for (const strategy of ['SHORTEST', 'FASTEST', 'CHEAPEST'] as const) {
      const task = await staffApi.planRoute(row.id, { strategy, fromLon: ep.fromLon, fromLat: ep.fromLat, toLon: ep.toLon, toLat: ep.toLat })
      results.push(task)
    }
    recommendations.value = results
    const best = [...results].sort((a, b) => (a.distanceM ?? 1e9) - (b.distanceM ?? 1e9))[0]
    currentTask.value = best
    activeOrderId.value = row.id
    await loadTrackPoints(row.id)
  } catch (e) {
    ElMessage.error((e as Error).message || '路线推荐失败')
  }
}

const searchViaPts = ref<number[][]>([])
function useDemandAsRoute(row: Demand) {
  splitRegion(row.originRegion)
  splitRegion2(row.targetRegion)
  searchVia.value = ''
  searchAvoid.value = ''
  tab.value = 'routes'
  onSearchRoute()
}
// 把 '省 市' 或 '省' 拆进 起点 级联
function splitRegion(region: string | null | undefined) {
  const [prov, city] = (region ?? '').split(' ')
  searchStartProv.value = prov && provinceOptions.includes(prov) ? prov : ''
  searchStartCity.value = city ? city : ''
}
function splitRegion2(region: string | null | undefined) {
  const [prov, city] = (region ?? '').split(' ')
  searchEndProv.value = prov && provinceOptions.includes(prov) ? prov : ''
  searchEndCity.value = city ? city : ''
}

async function onSearchRoute() {
  // 起/终点解析：优先城市中心，退回省级中心
  const start = centerOf(searchStartCity.value || searchStartProv.value)
  const end = centerOf(searchEndCity.value || searchEndProv.value)
  const viaNames = searchVia.value.split(/[,，]/).map((t) => t.trim()).filter(Boolean)
  const via = viaNames.map((c) => centerOf(c))
  const avoid = searchAvoid.value.split(/[,，]/).map((t) => t.trim()).filter(Boolean)
  searchViaPts.value = via
  const dist = haversineKm(start, end)
  const eta = Math.round((dist / 65) * 60)
  searchStartCoord.value = start
  searchEndCoord.value = end
  searchDetailOpen.value = false
  searchResult.value = { distanceKm: dist, etaMinutes: eta, via: viaNames }
  // 仅当存在可规划订单时调 GIS 引擎；不把搜索坐标强行写进无关订单
  const target = orders.value.find((o) => o.fromLon && o.toLon) ?? orders.value[0]
  if (target) {
    try {
      await staffApi.setEndpoints(target.id, { fromLon: start[0], fromLat: start[1], toLon: end[0], toLat: end[1] })
      const task = await staffApi.planRoute(target.id, { strategy: 'FASTEST', via, avoid })
      currentTask.value = task
      activeOrderId.value = target.id
      await loadTrackPoints(target.id)
    } catch (e) {
      ElMessage.warning('路线引擎规划失败，已按估算展示：' + ((e as Error).message || ''))
    }
  }
}

async function submitQuote() {
  if (!quoteRow.value || quotePrice.value < 0) return
  quoteSending.value = true
  try {
    await staffApi.quote(quoteRow.value.id, quotePrice.value)
    ElMessage.success('报价已提交，用户确认后成单')
    quoteOpen.value = false
    await load()
  } finally {
    quoteSending.value = false
  }
}


// 解析订单起终点坐标：优先订单已存坐标；否则按关联需求区域名推导（走 /orders/{id}/regions）并写回
async function resolveEndpoints(row: Order): Promise<{ fromLon: number; fromLat: number; toLon: number; toLat: number } | null> {
  let fLon = row.fromLon, fLat = row.fromLat, tLon = row.toLon, tLat = row.toLat
  if (fLon != null && fLat != null && tLon != null && tLat != null) {
    return { fromLon: fLon, fromLat: fLat, toLon: tLon, toLat: tLat }
  }
  const regions = await staffApi.orderRegions(row.id).catch(() => null)
  if (regions && (regions.originRegion || regions.targetRegion)) {
    const f = centerOf(regions.originRegion)
    const t = centerOf(regions.targetRegion)
    fLon = f[0]; fLat = f[1]; tLon = t[0]; tLat = t[1]
    await staffApi.setEndpoints(row.id, { fromLon: fLon, fromLat: fLat, toLon: tLon, toLat: tLat })
    ElMessage.success('已按起收区域自动定位起终点')
    return { fromLon: fLon, fromLat: fLat, toLon: tLon, toLat: tLat }
  }
  return null
}

async function onPlanRoute(row: Order) {
  const ep = await resolveEndpoints(row)
  if (!ep) {
    ElMessage.warning('订单 #' + row.id + ' 缺少起终点坐标，请在「路线搜索」选择起终点')
    tab.value = 'routes'
    return
  }
  try {
    const task = await staffApi.planRoute(row.id, { strategy: 'FASTEST', fromLon: ep.fromLon, fromLat: ep.fromLat, toLon: ep.toLon, toLat: ep.toLat })
    currentTask.value = task
    activeOrderId.value = row.id
    trackPoints.value = await loadTrackPoints(row.id)
    ElMessage.success(`路线规划完成：距离 ${task.distanceM}m，ETA ${task.etaS}s`)
    await load()
  } catch (e) {
    ElMessage.error('路线规划失败：' + (e instanceof Error ? e.message : ''))
  }
}

async function onPublishEvent(row: Order) {
  evOrder.value = row.id
  trackPoints.value = await loadTrackPoints(row.id)
  activeOrderId.value = row.id
  tab.value = 'events'
}

function isTerminalStatus(status: string) {
  return status === 'DELIVERED' || status === 'CANCELLED' || status === 'REFUNDED'
}
const currentEvOrderStatus = computed(() => {
  const o = orders.value.find((x) => x.id === evOrder.value)
  return o?.status ?? ''
})
function onEvOrderChange() {
  evType.value = 'IN_TRANSIT'
}

async function onPublishEvent2() {
  if (evOrder.value == null) return ElMessage.warning('请选择订单')
  if (isTerminalStatus(currentEvOrderStatus.value)) return ElMessage.warning('该订单已是终态，无法发布物流动态')
  evSending.value = true
  try {
    let lon: number | undefined
    let lat: number | undefined
    if (evCoords.value.trim()) {
      const parts = evCoords.value.trim().replace(/,/g, ' ').split(/\s+/).map((s) => Number(s))
      lon = parts[0]
      lat = parts[1]
    }
    await staffApi.publishEvent(evOrder.value, { type: evType.value, lon, lat })
    activeOrderId.value = evOrder.value
    trackPoints.value = await loadTrackPoints(evOrder.value)
    evHistory.value = await staffApi.orderEvents(evOrder.value)
    evCoords.value = ''
    ElMessage.success('物流动态已发布')
  } catch (e) {
    ElMessage.error('发布失败：' + (e instanceof Error ? e.message : ''))
  }
  evSending.value = false
}

function eventLabel(t: string) {
  const map: Record<string, string> = {
    PICKED: '快递员取件', IN_TRANSIT: '配送在途', ARRIVED_DELIVERY: '到达网点',
    ARRIVED: '到达网点', DELIVERED: '已签收', PENDING: '订单创建', PAID: '已支付',
  }
  return map[t] ?? t
}

function nextStatus(cur: string): string | null {
  const flow: Record<string, string> = { PAID: 'PICKED', PICKED: 'IN_TRANSIT', IN_TRANSIT: 'ARRIVED', ARRIVED: 'DELIVERED' }
  return flow[cur] ?? null
}
function canAdvance(s: string) {
  return ['PAID', 'PICKED', 'IN_TRANSIT', 'ARRIVED'].includes(s)
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '配送中',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
  }
  return map[s] ?? s
}

function statusTagType(s: string) {
  if (s === 'DELIVERED' || s === 'ARRIVED') return 'success'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'danger'
  if (s === 'IN_TRANSIT' || s === 'PICKED') return 'warning'
  if (s === 'PAID') return 'primary'
  return 'info'
}

async function advanceOrder(row: Order) {
  const nxt = nextStatus(row.status)
  if (!nxt) {
    ElMessage.warning('需先由用户支付，员工才能揽收配送')
    return
  }
  try {
    await staffApi.transition(row.id, nxt as OrderStatus)
    await load()
    ElMessage.success(`订单 ${row.id} 已推进至 ${statusLabel(nxt)}`)
  } catch (e) {
    ElMessage.error('推进失败：' + (e instanceof Error ? e.message : ''))
  }
}

// 编辑资料 / 改密码
const editName = ref('')
const editLicense = ref('')
const profileSaving = ref(false)
const pwdOld = ref('')
const pwdNew = ref('')
const pwdConfirm = ref('')
const pwdSaving = ref(false)

function syncEditFromProfile() {
  if (profile.value) {
    editName.value = profile.value.name || ''
    editLicense.value = profile.value.licenseNo || ''
  }
}

async function saveProfile() {
  profileSaving.value = true
  try {
    await staffApi.updateProfile(editName.value.trim() || (profile.value?.name ?? ''))
    if (editLicense.value.trim() && editLicense.value !== profile.value?.licenseNo) {
      await staffApi.updateLicense(editLicense.value.trim())
    }
    ElMessage.success('资料已保存')
    await loadProfile()
    syncEditFromProfile()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    profileSaving.value = false
  }
}

async function changePassword() {
  if (!pwdOld.value || !pwdNew.value || !pwdConfirm.value) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdNew.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdNew.value !== pwdConfirm.value) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await staffApi.changePassword(pwdOld.value, pwdNew.value)
    ElMessage.success('密码已修改，请下次登录使用新密码')
    pwdOld.value = ''; pwdNew.value = ''; pwdConfirm.value = ''
  } catch (e) {
    ElMessage.error((e as Error).message || '修改失败')
  } finally {
    pwdSaving.value = false
  }
}

async function loadProfile() {
  const pf = await staffApi.myProfile()
  profile.value = pf
  if (pf.siteName) deliveryArea.value = pf.siteName
  syncEditFromProfile()
}

async function loadReviews() {
  reviews.value = await staffApi.myReviews()
}

async function onReply(row: Review) {
  const text = (replyDrafts.value[row.id] || '').trim()
  if (!text) return
  await staffApi.replyReview(row.id, text)
  ElMessage.success('回复成功')
  await loadReviews()
}

function logout() {
  auth.logout()
  router.push('/user/login')
}

onMounted(() => {
  load()
})
</script>

<style scoped>
/* ============================================================
 * 员工运营台 · 深海军蓝 #1e3a5f + 琥珀 #d98e2b
 * 命令式顶栏 / Bento KPI / 白面板双层软阴影 / Stagger 入场
 * 仅本页局部生效，不动全局令牌
 * ============================================================ */
.staff-wrap {
  --sf-navy: #1e3a5f;
  --sf-navy-deep: #142a45;
  --sf-navy-soft: #35577f;
  --sf-amber: #d98e2b;
  --sf-amber-soft: #f3e3c8;
  --sf-canvas: #f4f6f9;
  --sf-panel: #ffffff;
  --sf-line: rgba(30, 58, 95, 0.12);
  --sf-line-strong: rgba(30, 58, 95, 0.28);
  --sf-text-hi: #18202b;
  --sf-text-mid: #52606f;
  --sf-text-low: #8b98a8;
  min-height: calc(100vh - 108px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 顶栏：品牌 + 命令 tab + 状态元信息 ---------- */
.staff-commandbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: var(--sf-panel);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.05), 0 8px 24px rgba(30, 58, 95, 0.07);
}
.staff-commandbar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--sf-navy);
  padding-right: 14px;
  border-right: 1px solid var(--sf-line);
}
.staff-commandbar__name { display: flex; flex-direction: column; gap: 2px; line-height: 1.15; }
.staff-commandbar__name strong { font-size: 14.5px; font-weight: 700; color: var(--sf-navy); letter-spacing: 0.02em; }
.staff-commandbar__name span { font-size: 9px; letter-spacing: 0.18em; color: var(--sf-text-low); }
.staff-commandbar__tabs {
  display: flex;
  gap: 2px;
  overflow-x: auto;
  flex: 1;
  min-width: 0;
  scrollbar-width: none;
}
.staff-commandbar__tabs::-webkit-scrollbar { display: none; }
.staff-tab {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 13px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  border-radius: 8px 8px 0 0;
  color: var(--sf-text-mid);
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 500;
  font-family: inherit;
  white-space: nowrap;
  transition: color 0.18s var(--ease), border-color 0.18s var(--ease), background 0.18s var(--ease);
}
.staff-tab:hover { color: var(--sf-text-hi); background: rgba(30, 58, 95, 0.045); }
.staff-tab.active {
  color: var(--sf-navy);
  border-bottom-color: var(--sf-amber);
  font-weight: 600;
  background: rgba(30, 58, 95, 0.035);
}
.staff-tab__count {
  font-size: 11px;
  font-weight: 700;
  color: var(--sf-navy);
  background: rgba(30, 58, 95, 0.09);
  padding: 1px 7px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}
.staff-tab.active .staff-tab__count { background: var(--sf-navy); color: #fff; }
.staff-commandbar__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 14px;
  border-left: 1px solid var(--sf-line);
}
.staff-commandbar__meta span { font-size: 11px; color: var(--sf-text-low); letter-spacing: 0.06em; }
.staff-logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid var(--sf-line-strong);
  color: var(--sf-text-mid);
  border-radius: 9px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 12.5px;
  font-weight: 600;
  font-family: inherit;
  transition: color 0.16s var(--ease), border-color 0.16s var(--ease), background 0.16s var(--ease);
}
.staff-logout:hover { color: #d64545; border-color: rgba(214, 69, 69, 0.5); background: rgba(214, 69, 69, 0.05); }

/* ---------- 模块入场节奏 ---------- */
.staff-wrap section > * {
  animation: sf-reveal 0.4s var(--ease) both;
}
.staff-wrap section > :nth-child(2) { animation-delay: 0.06s; }
.staff-wrap section > :nth-child(3) { animation-delay: 0.12s; }
@keyframes sf-reveal {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .staff-wrap section > * { animation: none; }
}

/* ---------- Bento KPI ---------- */
.staff-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
}
.staff-kpi {
  position: relative;
  padding: 16px 18px;
  overflow: hidden;
  background: var(--sf-panel);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.05), 0 8px 24px rgba(30, 58, 95, 0.07);
}
.staff-kpi::before {
  content: "";
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--sf-navy), rgba(30, 58, 95, 0.25));
}
.staff-kpi.accent { background: var(--sf-navy); border-color: var(--sf-navy-deep); }
.staff-kpi.accent::before { background: var(--sf-amber); width: 4px; }
.staff-kpi.accent .staff-kpi__num { color: #fff; }
.staff-kpi.accent .staff-kpi__label { color: rgba(255, 255, 255, 0.66); }
.staff-kpi__top { display: flex; justify-content: space-between; margin-bottom: 10px; }
.staff-kpi__tag {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--sf-navy-soft);
  background: rgba(30, 58, 95, 0.07);
  padding: 3px 8px;
  border-radius: 5px;
}
.staff-kpi.accent .staff-kpi__tag { color: var(--sf-amber); background: rgba(217, 142, 43, 0.18); }
.staff-kpi__num {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  color: var(--sf-navy);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}
.staff-kpi__label { margin-top: 9px; font-size: 12px; color: var(--sf-text-mid); }

/* ---------- 面板：白底 + 海军蓝细边 + 双层软阴影 ---------- */
.staff-panel {
  padding: 20px 22px;
  background: var(--sf-panel);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.05), 0 8px 24px rgba(30, 58, 95, 0.07);
}
.staff-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--sf-line-strong);
}
.staff-panel__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--sf-navy);
  display: flex;
  align-items: center;
  gap: 8px;
}
.staff-panel__title::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--sf-amber);
  box-shadow: 0 0 0 2.5px rgba(217, 142, 43, 0.22);
}
.staff-panel__hint { font-size: 12px; color: var(--sf-text-mid); }

/* ---------- 筛选条 ---------- */
.staff-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 4px 0 16px;
  padding: 10px 12px;
  background: rgba(30, 58, 95, 0.035);
  border: 1px solid var(--sf-line);
  border-radius: 12px;
}
.staff-filter__sel { width: 150px; }
.staff-filter__num { width: 140px; }
.staff-filter__switch { margin-left: 4px; }
.staff-filter__reset {
  margin-left: auto;
  background: none;
  border: 1px solid var(--sf-line-strong);
  color: var(--sf-text-mid);
  border-radius: 8px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s var(--ease);
  font-family: inherit;
}
.staff-filter__reset:hover { border-color: var(--sf-navy); color: var(--sf-navy); background: rgba(30, 58, 95, 0.05); }

/* ---------- 需求展开详情 ---------- */
.staff-demand-detail {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px 18px;
  font-size: 12.5px;
  color: var(--sf-text-mid);
  background: rgba(30, 58, 95, 0.028);
}
.staff-demand-detail__row { display: flex; gap: 14px; }
.staff-demand-detail__row span { flex-shrink: 0; width: 70px; color: var(--sf-text-low); letter-spacing: 0.04em; }
.staff-demand-detail__row b { color: var(--sf-text-hi); font-weight: 600; }
.staff-demand-detail__row b.ok { color: #147a57; }
.staff-demand-detail__row b.warn { color: #b45309; }
.staff-demand-detail__hint { margin-left: 8px; font-size: 11px; color: var(--sf-text-low); }
.staff-demand-detail__row b { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.staff-demand-detail__actions .el-button + .el-button { margin-left: 0; }

/* ---------- 表格精修 ---------- */
.staff-table { width: 100%; }
.staff-wrap .staff-table :deep(.el-table__header th.el-table__cell) {
  background: rgba(30, 58, 95, 0.055) !important;
  color: var(--sf-navy) !important;
  font-weight: 700;
  font-size: 12.5px;
  letter-spacing: 0.02em;
  border-bottom: 1.5px solid var(--sf-line-strong) !important;
}
.staff-wrap .staff-table :deep(.el-table__row) td { border-bottom: 1px solid rgba(30, 58, 95, 0.06); }
.staff-wrap .staff-table :deep(.el-table__row:nth-child(even)) td { background: rgba(30, 58, 95, 0.022); }
.staff-wrap .staff-table :deep(.el-table__row:hover td) { background: rgba(30, 58, 95, 0.06) !important; }

/* ---------- 双栏：地图 + 列表 ---------- */
.staff-ordergrid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 1200px) { .staff-ordergrid { grid-template-columns: 1fr; } }
.staff-mapglass { overflow: hidden; min-height: 460px; display: flex; flex-direction: column; background: var(--sf-panel); }
.staff-maphead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 18px;
  border-bottom: 1px solid var(--sf-line);
}
.staff-maphead__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--sf-navy);
  letter-spacing: 0.03em;
  display: flex;
  align-items: center;
  gap: 8px;
}
.staff-maphead__title::before {
  content: "";
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--sf-amber);
  box-shadow: 0 0 0 2.5px rgba(217, 142, 43, 0.22);
}
.staff-sse { display: flex; align-items: center; gap: 8px; font-size: 11px; font-weight: 700; color: var(--sf-text-mid); letter-spacing: 0.08em; }
.staff-mapcanvas { flex: 1; min-height: 400px; }
.staff-mapempty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--sf-text-low);
  font-size: 13px;
  min-height: 400px;
  letter-spacing: 0.04em;
}
.staff-orderslist { padding: 18px; background: var(--sf-panel); }

/* ---------- 路线搜索 ---------- */
.staff-routesearch {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  margin-bottom: 14px;
  background: rgba(30, 58, 95, 0.035);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
}
.staff-routesearch__head { display: flex; align-items: baseline; gap: 10px; }
.staff-routesearch__row { display: flex; gap: 10px; }
.staff-routesearch__row--split { flex-wrap: wrap; }
.staff-routesearch__in--prov { flex: 0 0 120px; }
.staff-routesearch__row--split .staff-routesearch__in:not(.staff-routesearch__in--prov) { flex: 1 1 140px; }
.staff-routesearch__in { flex: 1; }
.staff-routesearch__go { align-self: flex-start; }
.staff-routesearch__main { display: flex; gap: 14px; align-items: center; }
.staff-routesearch__main span { color: var(--sf-navy); font-weight: 600; }
.staff-routesearch__detail {
  margin-top: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--sf-line);
  border-radius: 10px;
}
.staff-routesearch__drow { display: flex; gap: 12px; font-size: 12.5px; color: var(--sf-text-mid); }
.staff-routesearch__drow span { flex-shrink: 0; width: 72px; color: var(--sf-text-low); }
.staff-routesearch__drow b { color: var(--sf-text-hi); font-weight: 600; }
.staff-routesearch__drow b.warn { color: #b45309; }
.staff-routesearch__result {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--sf-text-mid);
  padding: 8px 12px;
  background: rgba(30, 58, 95, 0.06);
  border-radius: 8px;
  border: 1px solid var(--sf-line);
}
.staff-routesearch__result span { color: var(--sf-navy); font-weight: 600; }
.staff-routesearch__avoid { margin-top: 6px; font-size: 11.5px; color: var(--sf-text-mid); }

/* ---------- 操作行：等宽工整 ---------- */
.staff-actrow { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.staff-actrow .el-button { min-width: 92px; justify-content: center; }

/* ---------- 路线推荐三卡 ---------- */
.staff-recs {
  margin-top: 14px;
  padding: 14px;
  background: rgba(30, 58, 95, 0.035);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
}
.staff-recs__head { margin-bottom: 12px; }
.staff-recs__grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
@media (max-width: 900px) { .staff-recs__grid { grid-template-columns: 1fr; } }
.staff-rec {
  padding: 14px;
  border: 1px solid var(--sf-line);
  border-radius: 12px;
  background: var(--sf-panel);
  transition: all 0.2s var(--ease);
}
.staff-rec--best {
  border-color: var(--sf-navy);
  background: linear-gradient(180deg, var(--sf-navy), var(--sf-navy-deep));
  box-shadow: 0 8px 22px rgba(20, 42, 69, 0.3);
}
.staff-rec--best .staff-rec__top b { color: #fff; }
.staff-rec--best .staff-rec__metrics span { color: var(--sf-amber); }
.staff-rec--best .staff-rec__hint { color: rgba(255, 255, 255, 0.55); }
.staff-rec__top { display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 10px; color: var(--sf-navy); font-weight: 700; }
.staff-rec__metrics { display: flex; gap: 14px; font-size: 14px; color: var(--sf-text-hi); font-weight: 600; }
.staff-rec__hint { font-size: 10.5px; color: var(--sf-text-low); margin-top: 6px; letter-spacing: 0.04em; }

/* ---------- 物流动态表单 ---------- */
.staff-eventform { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; }
.staff-eventform__sel { width: 180px; }
.staff-eventform__coords { width: 100%; }

/* ---------- 个人中心：头像 + Bento 统计 ---------- */
.staff-profile { display: flex; flex-direction: column; gap: 20px; padding: 4px 2px; }
.staff-profile__ident { display: flex; align-items: center; gap: 16px; }
.staff-profile__avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--sf-navy), var(--sf-navy-deep));
  color: #fff;
  font-size: 21px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 6px 16px rgba(20, 42, 69, 0.25);
}
.staff-profile__name { font-size: 18px; font-weight: 700; color: var(--sf-text-hi); }
.staff-profile__sub { font-size: 12px; color: var(--sf-text-mid); margin-top: 4px; }
.staff-bento {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}
.staff-stat {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  overflow: hidden;
  background: var(--sf-panel);
  border: 1px solid var(--sf-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.05), 0 8px 24px rgba(30, 58, 95, 0.07);
}
.staff-stat::before {
  content: "";
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--sf-navy), rgba(30, 58, 95, 0.25));
}
.staff-stat--hero { background: var(--sf-navy); border-color: var(--sf-navy-deep); }
.staff-stat--hero::before { background: var(--sf-amber); width: 4px; }
.staff-stat--hero .staff-stat__num { color: #fff; }
.staff-stat--hero .staff-stat__label { color: rgba(255, 255, 255, 0.66); }
.staff-stat--accent::before { background: var(--sf-amber); }
.staff-stat__tag {
  position: absolute;
  top: 12px; right: 14px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--sf-navy-soft);
  background: rgba(30, 58, 95, 0.08);
  padding: 2px 7px;
  border-radius: 4px;
}
.staff-stat--hero .staff-stat__tag { color: var(--sf-amber); background: rgba(217, 142, 43, 0.2); }
.staff-stat__num { font-size: 28px; font-weight: 700; color: var(--sf-navy); font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }
.staff-stat__label { font-size: 11.5px; color: var(--sf-text-mid); letter-spacing: 0.04em; }
.staff-profile__edit {
  margin-top: 2px;
  padding: 14px 16px;
  border: 1px solid var(--sf-line);
  border-radius: 14px;
  background: rgba(30, 58, 95, 0.03);
}
.staff-profile__edit-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--sf-navy);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.staff-profile__edit-title::before { content: ""; width: 5px; height: 5px; border-radius: 50%; background: var(--sf-amber); }
.staff-profile__edit-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.staff-profile__edit-in { flex: 1 1 160px; min-width: 120px; }

/* ---------- 报价 dialog ---------- */
.staff-quote { display: flex; flex-direction: column; gap: 14px; }
.staff-quote__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: rgba(30, 58, 95, 0.045);
  border: 1px solid var(--sf-line);
  border-radius: 12px;
}
.staff-quote__row { display: flex; justify-content: space-between; font-size: 13px; color: var(--sf-text-mid); }
.staff-quote__row b { color: var(--sf-text-hi); font-weight: 600; text-align: right; }
.staff-quote__input { width: 100%; }
.staff-quote__unit { font-size: 11.5px; color: var(--sf-text-low); margin-top: -6px; }
.staff-quote__row .quote-hot { color: #b45309; }
.staff-quote__suggest {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--sf-navy);
  border-radius: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.staff-quote__suggest b { color: var(--sf-amber); font-weight: 700; }

/* ---------- 星级 / 回复 ---------- */
.staff-star { color: #d98e2b; font-size: 13px; letter-spacing: 1px; }
.staff-reply { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--sf-text-hi); line-height: 1.5; flex-wrap: wrap; }
.staff-reply--empty { color: var(--sf-text-low); }
.staff-reply__tag {
  flex-shrink: 0;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(217, 142, 43, 0.14);
  color: #b45309;
}
.staff-reply-input { margin: 6px 0; }
.staff-ep-none { color: var(--sf-text-low); font-size: 11px; }
.staff-ep-region { font-size: 13px; font-weight: 600; color: var(--sf-text-hi); letter-spacing: 0.01em; }
</style>
